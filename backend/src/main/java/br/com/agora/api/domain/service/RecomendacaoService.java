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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.Normalizer;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Eu combino sinais do usuário e recência para montar o feed personalizado. */
@Service
public class RecomendacaoService {
    // Inicio em 15% para personalizar mesmo com pouco histórico; preservo peso para recência.
    private static final double BETA_MINIMO = 0.15;
    private static final double BETA_MAXIMO = 0.85;
    // 8 é um parâmetro inicial para controlar quanto histórico faz o beta crescer.
    private static final double FATOR_CONFIANCA = 8.0;
    // Eu reduzo pela metade o peso de um feedback a cada 30 dias.
    private static final double MEIA_VIDA_DIAS = 30.0;
    // Eu uso 10 dias como escala para a notícia perder força conforme envelhece.
    private static final double ESCALA_RECENCIA_DIAS = 10.0;
    // Dou mais peso ao embedding; os termos literais servem de complemento simples.
    private static final double PESO_EMBEDDING = 0.8;
    private static final double PESO_TERMOS = 0.2;
    private static final Set<String> STOPWORDS = Set.of("para", "com", "uma", "mais", "sobre", "entre", "apos", "antes",
            "como", "pela", "pelo", "suas", "seus", "que", "por", "dos", "das", "nos", "nas",
            "eles", "elas", "seu", "sua", "foi", "ser", "sao", "tem", "ter", "tambem", "esta",
            "esse", "essa", "isso", "quando", "onde", "the", "and", "for", "with", "from");

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
        // Tento processar o que falta sem consultar a NewsAPI; se o modelo falhar, uso texto/recência.
        newsService.prepararPersonalizacao();
        List<FeedbackEvento> eventos = eventoRepository.findTop500ByUsuarioIdOrderByCriadoEmDesc(usuarioId);
        List<Long> idsNoticiados = eventos.stream().map(FeedbackEvento::getNewsId).distinct().toList();
        Map<Long, News> noticiasDoHistorico = newsRepository.findAllById(idsNoticiados).stream()
                .collect(Collectors.toMap(News::getId, Function.identity()));
        Map<Long, Double> preferencias = new HashMap<>();
        Map<String, Double> termosPositivos = new HashMap<>();
        Map<String, Double> termosNegativos = new HashMap<>();
        double[] vetorPositivo = null;
        double[] vetorNegativo = null;
        double evidencias = 0;
        int sinaisConsiderados = 0;
        int sinaisPendentes = 0;
        LocalDateTime agora = LocalDateTime.now();

        for (FeedbackEvento evento : eventos) {
            double peso = peso(evento.getTipo());
            if (peso == 0) continue;
            News origem = noticiasDoHistorico.get(evento.getNewsId());
            Long clusterId = evento.getClusterId() != null ? evento.getClusterId()
                    : origem != null ? origem.getClusterId() : null;
            if (clusterId == null && origem == null) {
                sinaisPendentes++;
                continue;
            }
            sinaisConsiderados++;
            double idadeDias = Math.max(0, ChronoUnit.MINUTES.between(evento.getCriadoEm(), agora) / 1440.0);
            double decaimento = Math.pow(0.5, idadeDias / MEIA_VIDA_DIAS);
            double pesoAtual = peso * decaimento;
            if (clusterId != null) preferencias.merge(clusterId, pesoAtual, Double::sum);
            evidencias += Math.abs(pesoAtual);

            if (origem != null) {
                adicionarTermos(termosPositivos, termosNegativos, textoDaNoticia(origem), pesoAtual);
                List<Double> embedding = origem.getEmbedding();
                if (embedding != null && !embedding.isEmpty()) {
                    int dimensao = embedding.size();
                    if (vetorPositivo == null) vetorPositivo = new double[dimensao];
                    if (vetorNegativo == null) vetorNegativo = new double[dimensao];
                    if (vetorPositivo.length == dimensao && vetorNegativo.length == dimensao) {
                        double[] destino = pesoAtual > 0 ? vetorPositivo : vetorNegativo;
                        for (int i = 0; i < dimensao; i++) {
                            if (embedding.get(i) != null) destino[i] += embedding.get(i) * Math.abs(pesoAtual);
                        }
                    }
                }
            }
        }

        // Beta depende da quantidade de evidência; o sinal positivo/negativo molda o perfil.
        double beta = BETA_MINIMO + (BETA_MAXIMO - BETA_MINIMO)
                * (1.0 - Math.exp(-evidencias / FATOR_CONFIANCA));
        LocalDateTime referencia = agora;
        // Incluo notícias sem embedding para o feed não depender de todo o catálogo estar processado.
        List<News> candidatas = newsRepository.findTop1000ByOrderByPublicadoEmDesc();
        if (candidatas.isEmpty()) candidatas = newsRepository.findTop100ByOrderByPublicadoEmDesc();
        double[] perfilPositivo = vetorPositivo;
        double[] perfilNegativo = vetorNegativo;
        List<News> ordenadas = candidatas.stream()
                .sorted(Comparator.comparingDouble((News noticia) -> pontuacao(
                                noticia, preferencias, perfilPositivo, perfilNegativo,
                                termosPositivos, termosNegativos, beta, referencia))
                        .reversed()
                        .thenComparing(News::getPublicadoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(60)
                .toList();

        return new RecomendacaoDTO(
                ordenadas.stream().map(newsService::toDTO).toList(),
                Math.round(beta * 1000.0) / 1000.0,
                sinaisConsiderados,
                sinaisPendentes
        );
    }

    private double pontuacao(News noticia, Map<Long, Double> preferencias,
                             double[] vetorPositivo, double[] vetorNegativo,
                             Map<String, Double> termosPositivos, Map<String, Double> termosNegativos,
                             double beta, LocalDateTime agora) {
        double idadeDias = noticia.getPublicadoEm() == null ? 30
                : Math.max(0, ChronoUnit.MINUTES.between(noticia.getPublicadoEm(), agora) / 1440.0);
        double recencia = Math.exp(-idadeDias / ESCALA_RECENCIA_DIAS);
        double relevancia = relevancia(noticia, preferencias, vetorPositivo, vetorNegativo,
                termosPositivos, termosNegativos);
        return (1.0 - beta) * recencia + beta * relevancia;
    }

    private double relevancia(News noticia, Map<Long, Double> preferencias,
                              double[] vetorPositivo, double[] vetorNegativo,
                              Map<String, Double> termosPositivos, Map<String, Double> termosNegativos) {
        double relevanciaCluster = noticia.getClusterId() == null ? 0.5
                : (Math.tanh(preferencias.getOrDefault(noticia.getClusterId(), 0.0) / 4.0) + 1.0) / 2.0;
        boolean temCluster = noticia.getClusterId() != null && !preferencias.isEmpty();

        Double relevanciaConteudo = relevanciaPorEmbedding(noticia.getEmbedding(), vetorPositivo, vetorNegativo);
        Double relevanciaTexto = relevanciaPorTermos(textoDaNoticia(noticia), termosPositivos, termosNegativos);
        if (relevanciaConteudo != null && relevanciaTexto != null) {
            relevanciaConteudo = relevanciaConteudo * PESO_EMBEDDING + relevanciaTexto * PESO_TERMOS;
        } else if (relevanciaConteudo == null) {
            relevanciaConteudo = relevanciaTexto;
        }

        if (relevanciaConteudo != null && temCluster) return relevanciaConteudo * 0.8 + relevanciaCluster * 0.2;
        if (relevanciaConteudo != null) return relevanciaConteudo;
        if (temCluster) return relevanciaCluster;
        return 0.5;
    }

    private Double relevanciaPorEmbedding(List<Double> embedding, double[] positivo, double[] negativo) {
        if (embedding == null || embedding.isEmpty() || (positivo == null && negativo == null)) return null;
        double similaridadePositiva = positivo == null ? 0.0 : cosseno(embedding, positivo);
        double similaridadeNegativa = negativo == null ? 0.0 : cosseno(embedding, negativo);
        return limitar(0.5 + 0.25 * (similaridadePositiva - similaridadeNegativa));
    }

    private double cosseno(List<Double> vetor, double[] perfil) {
        if (vetor.size() != perfil.length) return 0.0;
        double produto = 0.0, normaVetor = 0.0, normaPerfil = 0.0;
        for (int i = 0; i < perfil.length; i++) {
            double valor = vetor.get(i) == null ? 0.0 : vetor.get(i);
            produto += valor * perfil[i];
            normaVetor += valor * valor;
            normaPerfil += perfil[i] * perfil[i];
        }
        if (normaVetor == 0 || normaPerfil == 0) return 0.0;
        return produto / (Math.sqrt(normaVetor) * Math.sqrt(normaPerfil));
    }

    private Double relevanciaPorTermos(String texto, Map<String, Double> positivos, Map<String, Double> negativos) {
        if (positivos.isEmpty() && negativos.isEmpty()) return null;
        Set<String> termos = tokens(texto);
        if (termos.isEmpty()) return 0.5;
        double totalPositivo = positivos.values().stream().mapToDouble(Double::doubleValue).sum();
        double totalNegativo = negativos.values().stream().mapToDouble(Double::doubleValue).sum();
        double correspondenciaPositiva = termos.stream().mapToDouble(t -> positivos.getOrDefault(t, 0.0)).sum();
        double correspondenciaNegativa = termos.stream().mapToDouble(t -> negativos.getOrDefault(t, 0.0)).sum();
        double positiva = totalPositivo == 0 ? 0 : correspondenciaPositiva / totalPositivo;
        double negativa = totalNegativo == 0 ? 0 : correspondenciaNegativa / totalNegativo;
        return limitar(0.5 + 0.5 * (positiva - negativa));
    }

    private void adicionarTermos(Map<String, Double> positivos, Map<String, Double> negativos,
                                 String texto, double peso) {
        Map<String, Double> destino = peso > 0 ? positivos : negativos;
        for (String token : tokens(texto)) destino.merge(token, Math.abs(peso), Double::sum);
    }

    private Set<String> tokens(String texto) {
        String normalizado = Normalizer.normalize(texto == null ? "" : texto.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        Set<String> resultado = new HashSet<>();
        for (String token : normalizado.split("[^a-z0-9]+")) {
            if (token.length() >= 3 && !STOPWORDS.contains(token)) resultado.add(token);
        }
        return resultado;
    }

    private String textoDaNoticia(News noticia) {
        return (noticia.getTitulo() == null ? "" : noticia.getTitulo()) + " "
                + (noticia.getDescricao() == null ? "" : noticia.getDescricao());
    }

    private double limitar(double valor) {
        return Math.max(0.0, Math.min(1.0, valor));
    }

    private double peso(TipoFeedback tipo) {
        // Estes pesos são minha primeira escala de importância; ainda preciso avaliá-los com uso real.
        return switch (tipo) {
            case LIKE -> 3.0;
            case MORE -> 2.0;
            case SAVE -> 1.5;
            case OPEN_ARTICLE -> 1.0;
            case LESS -> -3.0;
            case UNLIKE -> -0.5;
            case UNSAVE -> -0.25;
            case CLEAR_MORE, CLEAR_LESS -> 0;
        };
    }
}
