package br.com.agora.api.domain.service;

import br.com.agora.api.domain.model.NewsApiColetaEstado;
import br.com.agora.api.domain.repository.NewsApiColetaEstadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

/** Eu concentro a coleta aqui para os usuários compartilharem os mesmos artigos e a mesma cota. */
@Service
public class NewsApiColetaService {
    private static final Logger log = LoggerFactory.getLogger(NewsApiColetaService.class);
    // Escolhi 60 artigos por chamada para formar um catálogo útil sem paginar muitas vezes.
    private static final int PAGE_SIZE = 60;
    // Limite local conservador; deixo margem para testes e posso configurá-lo pelo ambiente.
    private static final int LIMITE_DIARIO_PADRAO = 20;

    private final NewsService newsService;
    private final NewsApiColetaEstadoRepository estadoRepository;

    @Value("${newsapi.base-url}")
    private String baseUrl;

    @Value("${newsapi.collection.interval-hours:24}")
    private int intervaloHoras;

    @Value("${newsapi.collection.daily-budget:" + LIMITE_DIARIO_PADRAO + "}")
    private int limiteDiario;

    public NewsApiColetaService(NewsService newsService,
                                NewsApiColetaEstadoRepository estadoRepository) {
        this.newsService = newsService;
        this.estadoRepository = estadoRepository;
    }

    /** Quatro consultas temáticas compartilhadas; usuários reaproveitam as notícias do banco. */
    public synchronized void coletarTemasSeNecessarios() {
        // Evito duplicar consultas concorrentes dentro do mesmo processo do backend.
        // Estes assuntos são consultas iniciais definidas manualmente para ampliar o catálogo;
        // eu posso ajustá-los conforme a proposta e os dados disponíveis.
        for (Consulta consulta : List.of(
                new Consulta("tema:geral", "brasil", "pt", null, null, null),
                new Consulta("tema:politica", "política OR governo OR eleição", "pt", null, null, null),
                new Consulta("tema:esportes", "futebol OR esporte OR campeonato", "pt", null, null, null),
                new Consulta("tema:tecnologia", "tecnologia OR \"inteligência artificial\" OR celular", "pt", null, null, null)
        )) {
            coletarSeNecessario(consulta);
        }
        newsService.prepararPersonalizacao();
    }

    /** Portais são carregados sob demanda, mas no máximo uma vez por intervalo para todos. */
    public synchronized void coletarPortalSeNecessario(String portal) {
        coletarSeNecessario(consultaDoPortal(portal));
        newsService.prepararPersonalizacao();
    }

    // Eu agendo a tentativa para as 8h de Brasília; ela só roda com o backend ligado.
    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    public void coletaDiaria() {
        coletarTemasSeNecessarios();
    }

    private void coletarSeNecessario(Consulta consulta) {
        if (!newsService.isNewsApiConfigurada()) {
            log.warn("NewsAPI não configurada; mantendo notícias já armazenadas (consulta {}).", consulta.chave());
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        NewsApiColetaEstado estado = estadoRepository.findById(consulta.chave())
                .orElseGet(() -> novoEstado(consulta.chave()));
        LocalDateTime limite = agora.minusHours(Math.max(1, intervaloHoras));
        if (estado.getUltimaTentativa() != null && estado.getUltimaTentativa().isAfter(limite)) return;

        String chaveOrcamento = "orcamento:" + LocalDate.now(ZoneOffset.UTC);
        NewsApiColetaEstado orcamento = estadoRepository.findById(chaveOrcamento)
                .orElseGet(() -> novoEstado(chaveOrcamento));
        int requisicoesHoje = orcamento.getRequisicoesNoDia() == null ? 0 : orcamento.getRequisicoesNoDia();
        if (requisicoesHoje >= Math.max(1, limiteDiario)) {
            log.warn("Teto local de {} chamadas à NewsAPI atingido hoje; consulta {} adiada.", limiteDiario, consulta.chave());
            return;
        }

        // Registro a tentativa antes da chamada para não repeti-la em loop após uma falha/reinício.
        estado.setUltimaTentativa(agora);
        estadoRepository.saveAndFlush(estado);
        orcamento.setRequisicoesNoDia(requisicoesHoje + 1);
        orcamento.setUltimaTentativa(agora);
        estadoRepository.saveAndFlush(orcamento);

        try {
            int quantidade = newsService.coletarDaNewsApi(montarUrl(consulta), consulta.rotuloPortal());
            estado.setUltimaColeta(agora);
            estado.setArtigosRecebidos(quantidade);
            estado.setUltimoErro(null);
            log.info("Coleta {} concluída: {} artigos recebidos. Uso local da NewsAPI: {}/{} no dia UTC.",
                    consulta.chave(), quantidade, requisicoesHoje + 1, limiteDiario);
        } catch (Exception e) {
            estado.setUltimoErro(resumir(e.getMessage()));
            log.warn("Falha na coleta {}. Nova tentativa após {} horas: {}",
                    consulta.chave(), intervaloHoras, e.getMessage());
        }
        estadoRepository.save(estado);
    }

    private String montarUrl(Consulta consulta) {
        LocalDate hojeUtc = LocalDate.now(ZoneOffset.UTC);
        // Busco até ontem porque o plano de desenvolvimento tem atraso na disponibilidade dos artigos.
        LocalDate inicioJanela = hojeUtc.minusDays(3);
        LocalDate fimJanela = hojeUtc.minusDays(1);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/everything")
                .queryParam("from", inicioJanela)
                .queryParam("to", fimJanela)
                .queryParam("language", consulta.idioma())
                .queryParam("sortBy", "publishedAt")
                .queryParam("pageSize", PAGE_SIZE);
        if (consulta.termo() != null) builder.queryParam("q", consulta.termo());
        if (consulta.fontes() != null) builder.queryParam("sources", consulta.fontes());
        if (consulta.dominios() != null) builder.queryParam("domains", consulta.dominios());
        return builder.build().encode().toUriString();
    }

    private Consulta consultaDoPortal(String portal) {
        String chave = normalizar(portal);
        return switch (chave) {
            case "globo - g1", "globo-g1", "globo g1", "g1", "globo" ->
                    new Consulta("portal:g1", "g1 OR globo", "pt", null, null, portal);
            case "metropoles" -> new Consulta("portal:metropoles", null, "pt", null, "metropoles.com", portal);
            case "uol" -> new Consulta("portal:uol", null, "pt", null, "uol.com.br", portal);
            case "estadao" -> new Consulta("portal:estadao", "estadao", "pt", null, null, portal);
            case "exame" -> new Consulta("portal:exame", null, "pt", null, "exame.com", portal);
            case "ign brasil" -> new Consulta("portal:ign", null, "pt", null, "ign.com", portal);
            case "bbc news" -> new Consulta("portal:bbc", null, "en", "bbc-news", null, portal);
            case "bloomberg" -> new Consulta("portal:bloomberg", null, "en", "bloomberg", null, portal);
            case "cnn" -> new Consulta("portal:cnn", null, "en", "cnn", null, portal);
            case "reuters" -> new Consulta("portal:reuters", "reuters", "en", null, null, portal);
            case "techcrunch" -> new Consulta("portal:techcrunch", null, "en", "techcrunch", null, portal);
            case "the new york times" -> new Consulta("portal:nyt", "\"new york times\"", "en", null, null, portal);
            default -> new Consulta("portal:busca:" + chave, portal, "pt", null, null, portal);
        };
    }

    private NewsApiColetaEstado novoEstado(String chave) {
        NewsApiColetaEstado estado = new NewsApiColetaEstado();
        estado.setChaveConsulta(chave);
        return estado;
    }

    private String normalizar(String valor) {
        return java.text.Normalizer.normalize(valor.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }

    private String resumir(String erro) {
        if (erro == null) return "Falha sem mensagem";
        return erro.length() <= 500 ? erro : erro.substring(0, 500);
    }

    private record Consulta(String chave, String termo, String idioma,
                            String fontes, String dominios, String rotuloPortal) { }
}
