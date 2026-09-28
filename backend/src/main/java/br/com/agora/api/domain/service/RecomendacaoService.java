package br.com.agora.api.domain.service;

import br.com.agora.api.controller.dto.NoticiaDTO;
import br.com.agora.api.controller.dto.RecomendacaoDTO;
import br.com.agora.api.domain.model.FeedbackEvento;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.model.TipoFeedback;
import br.com.agora.api.domain.repository.FeedbackEventoRepository;
import br.com.agora.api.domain.repository.NewsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ordena notícias recentes usando evidências de feedback agrupadas por cluster. */
@Service
public class RecomendacaoService {
    private static final double BETA_MINIMO = 0.15;
    private static final double BETA_MAXIMO = 0.85;
    private static final double FATOR_CONFIANCA = 8.0;
    private static final double MEIA_VIDA_DIAS = 30.0;

    private final FeedbackEventoRepository eventoRepository;
    private final NewsRepository newsRepository;
    private final NewsService newsService;

    public RecomendacaoService(FeedbackEventoRepository eventoRepository,
                               NewsRepository newsRepository,
                               NewsService newsService) {
        this.eventoRepository = eventoRepository;
        this.newsRepository = newsRepository;
        this.newsService = newsService;
    }

    public RecomendacaoDTO paraUsuario(Long usuarioId) {
        // Também tenta processar o que ficou pendente, sem buscar novos artigos na NewsAPI.
        newsService.prepararPersonalizacao();
        List<FeedbackEvento> eventos = eventoRepository.findTop500ByUsuarioIdOrderByCriadoEmDesc(usuarioId);
        Map<Long, Long> clusterAtualPorNoticia = new HashMap<>();
        List<Long> noticiasSemSnapshot = eventos.stream()
                .filter(evento -> evento.getClusterId() == null)
                .map(FeedbackEvento::getNewsId)
                .distinct().toList();
        if (!noticiasSemSnapshot.isEmpty()) {
            newsRepository.findAllById(noticiasSemSnapshot).stream()
                    .filter(noticia -> noticia.getClusterId() != null)
                    .forEach(noticia -> clusterAtualPorNoticia.put(noticia.getId(), noticia.getClusterId()));
        }
        Map<Long, Double> preferencias = new HashMap<>();
        double evidencias = 0;
        int sinaisConsiderados = 0;
        int sinaisPendentes = 0;
        LocalDateTime agora = LocalDateTime.now();

        for (FeedbackEvento evento : eventos) {
            double peso = peso(evento.getTipo());
            if (peso == 0) continue;
            Long clusterId = evento.getClusterId() != null
                    ? evento.getClusterId() : clusterAtualPorNoticia.get(evento.getNewsId());
            if (clusterId == null) {
                sinaisPendentes++;
                continue;
            }
            sinaisConsiderados++;
            double idadeDias = Math.max(0, ChronoUnit.MINUTES.between(evento.getCriadoEm(), agora) / 1440.0);
            double decaimento = Math.pow(0.5, idadeDias / MEIA_VIDA_DIAS);
            preferencias.merge(clusterId, peso * decaimento, Double::sum);
            evidencias += Math.abs(peso) * decaimento;
        }

        double beta = BETA_MINIMO + (BETA_MAXIMO - BETA_MINIMO)
                * (1.0 - Math.exp(-evidencias / FATOR_CONFIANCA));
        LocalDateTime referencia = agora;
        List<News> candidatas = newsRepository.findTop100ByEmbeddingIsNotNullOrderByPublicadoEmDesc();
        // Mantém a seção utilizável se os vetores ainda não estiverem prontos ou o modelo falhar.
        if (candidatas.isEmpty()) candidatas = newsRepository.findTop100ByOrderByPublicadoEmDesc();
        List<News> ordenadas = candidatas.stream()
                .sorted(Comparator.comparingDouble((News noticia) -> pontuacao(noticia, preferencias, beta, referencia))
                        .reversed()
                        .thenComparing(News::getPublicadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        return new RecomendacaoDTO(
                ordenadas.stream().map(newsService::toDTO).toList(),
                Math.round(beta * 1000.0) / 1000.0,
                sinaisConsiderados,
                sinaisPendentes
        );
    }

    private double pontuacao(News noticia, Map<Long, Double> preferencias, double beta, LocalDateTime agora) {
        double idadeDias = noticia.getPublicadoEm() == null ? 30
                : Math.max(0, ChronoUnit.MINUTES.between(noticia.getPublicadoEm(), agora) / 1440.0);
        double recencia = Math.exp(-idadeDias / 10.0);
        double interesse = noticia.getClusterId() == null ? 0
                : Math.tanh(preferencias.getOrDefault(noticia.getClusterId(), 0.0) / 4.0);
        double relevancia = (interesse + 1.0) / 2.0;
        return (1.0 - beta) * recencia + beta * relevancia;
    }

    private double peso(TipoFeedback tipo) {
        return switch (tipo) {
            case LIKE -> 3.0;
            case MORE -> 2.0;
            case SAVE -> 1.5;
            case OPEN_ARTICLE -> 0.25;
            case LESS -> -3.0;
            case UNLIKE -> -0.5;
            case UNSAVE -> -0.25;
            case CLEAR_MORE, CLEAR_LESS -> 0;
        };
    }
}
