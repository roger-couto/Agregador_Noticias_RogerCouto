package br.com.agora.api.domain.service;

import br.com.agora.api.controller.dto.NoticiaDTO;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.repository.NewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {

    @Value("${newsapi.key}") // Lê a chave da newsapi
    private String apiKey;

    @Value("${newsapi.base-url}") // URL base do arq. application.properties
    private String baseUrl;

    private final NewsRepository newsRepository;
    private final EmbeddingClient embeddingClient;
    private final ClusterizacaoService clusterizacaoService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public NewsService(NewsRepository newsRepository, EmbeddingClient embeddingClient,
                       ClusterizacaoService clusterizacaoService) {
        this.newsRepository = newsRepository;
        this.embeddingClient = embeddingClient;
        this.clusterizacaoService = clusterizacaoService;
    }

    private static List<NoticiaDTO> cacheRecentes = null; // Cache em memória para feed
    private static long cacheRecentesTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutos

    public List<NoticiaDTO> buscarPorPortal(String portal) {
        String url;
        // Normalizar as palavras que vêm do front
        String portalChave = portal.trim().toLowerCase().replace("/", "-");
        portalChave = java.text.Normalizer.normalize(portalChave, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");

        switch (portalChave) {
            // NACIONAIS
            case "globo - g1", "globo-g1", "globo g1", "g1", "globo" ->
                    url = baseUrl + "/everything?q=g1 OR globo&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "metropoles" ->
                    url = baseUrl + "/everything?domains=metropoles.com&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "uol" ->
                    url = baseUrl + "/everything?domains=uol.com.br&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "estadao" ->
                    url = baseUrl + "/everything?q=estadao&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "exame" ->
                    url = baseUrl + "/everything?domains=exame.com&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "ign brasil" ->
                    url = baseUrl + "/everything?domains=ign.com&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;

            // INTERNACIONAIS (forçando language=en)
            case "bbc news" ->
                    url = baseUrl + "/everything?sources=bbc-news&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "bloomberg" ->
                    url = baseUrl + "/everything?sources=bloomberg&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "cnn" ->
                    url = baseUrl + "/everything?sources=cnn&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "reuters" ->
                    url = baseUrl + "/everything?q=reuters&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "techcrunch" ->
                    url = baseUrl + "/everything?sources=techcrunch&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "the new york times" ->
                    url = baseUrl + "/everything?q=\"new york times\"&language=en&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;

            default ->
                    url = baseUrl + "/everything?q=" + encode(portal) + "&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
        }

        return fetchEConverter(url, portal);
    }

    public List<NoticiaDTO> buscarRecentes() {
        long agora = System.currentTimeMillis();
        if (cacheRecentes != null && (agora - cacheRecentesTimestamp) < CACHE_TTL_MS) {
            System.out.println("[NewsService] Cache ativo, próxima atualização em "
                    + ((CACHE_TTL_MS - (agora - cacheRecentesTimestamp)) / 60000) + " min");
            return cacheRecentes;
        }

        String url = baseUrl + "/everything?q=brasil&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
        List<NoticiaDTO> resultado = fetchEConverter(url, null);
        if (!resultado.isEmpty()) {
            cacheRecentes = resultado;
            cacheRecentesTimestamp = agora;
            System.out.println("[NewsService] Cache de recentes updated");
        }
        return (resultado.isEmpty() && cacheRecentes != null) ? cacheRecentes : resultado;
    }

    private List<NoticiaDTO> fetchEConverter(String url, String portal) {
        List<News> resultado = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode articles = root.path("articles");

            for (JsonNode node : articles) {
                String titulo = node.path("title").asText("");
                String urlNoticia = node.path("url").asText("");
                if (titulo.isBlank() || titulo.equals("null") || titulo.equals("[Removed]") || urlNoticia.isBlank()) continue;

                String descricao = node.path("description").asText("");
                if (descricao.equals("null")) descricao = "";
                String imageUrl = node.path("urlToImage").asText("");
                if (imageUrl.equals("null")) imageUrl = "";

                final String tituloFinal = titulo;
                final String descricaoFinal = descricao;
                final String imageUrlFinal = imageUrl;

                News news = newsRepository.findByUrl(urlNoticia).map(existente -> {
                    boolean tituloInvalidoSalvo = existente.getTitulo() == null
                            || existente.getTitulo().isBlank()
                            || existente.getTitulo().equals("null");
                    if (tituloInvalidoSalvo) {
                        existente.setTitulo(tituloFinal);
                        existente.setDescricao(descricaoFinal);
                        existente.setImageUrl(imageUrlFinal);
                        return newsRepository.save(existente);
                    }
                    return existente;
                }).orElseGet(() -> {
                    News nova = new News();
                    nova.setTitulo(tituloFinal);
                    nova.setDescricao(descricaoFinal);
                    nova.setUrl(urlNoticia);
                    nova.setImageUrl(imageUrlFinal);
                    nova.setPortal(portal != null ? portal : node.path("source").path("name").asText(""));

                    String publishedAt = node.path("publishedAt").asText("");
                    if (!publishedAt.isBlank()) {
                        nova.setPublicadoEm(OffsetDateTime.parse(publishedAt).toLocalDateTime());
                    } else {
                        nova.setPublicadoEm(LocalDateTime.now());
                    }
                    return newsRepository.save(nova);
                });

                resultado.add(news);
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("[NewsService] NewsAPI retornou erro HTTP " + e.getStatusCode()
                    + " - " + e.getResponseBodyAsString());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("[NewsService] Falha de rede ao chamar NewsAPI: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[NewsService] Erro inesperado ao buscar noticias: " + e.getMessage());
            e.printStackTrace();
        }
        prepararPersonalizacao();
        return resultado.stream().map(news -> newsRepository.findById(news.getId()).orElse(news))
                .map(this::toDTO).toList();
    }

    /** Processa somente registros sem vetor; a operação não consulta a NewsAPI. */
    public void prepararPersonalizacao() {
        try {
            List<News> pendentes = newsRepository.findAllByEmbeddingIsNullAndTituloIsNotNullOrderByIdAsc()
                    .stream().filter(n -> n.getTitulo() != null && !n.getTitulo().isBlank())
                    .limit(128).toList();
            if (!pendentes.isEmpty()) {
                List<String> textos = pendentes.stream().map(n -> {
                    String resumo = n.getDescricao() == null ? "" : n.getDescricao().trim();
                    return n.getTitulo().trim() + (resumo.isBlank() ? "" : "\n" + resumo);
                }).toList();
                var resposta = embeddingClient.gerarEmbeddings(textos);
                if (resposta == null || resposta.embeddings() == null || resposta.embeddings().size() != pendentes.size()) {
                    throw new IllegalStateException("Resposta do serviço de embeddings não corresponde ao lote enviado");
                }
                for (int i = 0; i < pendentes.size(); i++) {
                    News news = pendentes.get(i);
                    news.setEmbedding(resposta.embeddings().get(i));
                    news.setEmbeddingModel(resposta.model());
                    news.setEmbeddingRevision(resposta.revision());
                }
                newsRepository.saveAll(pendentes);
            }
            clusterizacaoService.atualizarClusters();
        } catch (Exception e) {
            // A indisponibilidade do modelo não deve impedir a entrega das notícias.
            System.err.println("[Personalizacao] Embeddings/clusters adiados: " + e.getMessage());
        }
    }

    public NoticiaDTO toDTO(News news) {
        return new NoticiaDTO(
                news.getId(), news.getTitulo(), news.getDescricao(),
                news.getUrl(), news.getImageUrl(), news.getPortal(),
                news.getPublicadoEm(), news.getGostei(), news.getLerDepois(), news.getClusterId()
        );
    }

    public NoticiaDTO curtirNoticia(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Noticia nao encontrada: " + id));
        news.setGostei(news.getGostei() + 1);
        return toDTO(newsRepository.save(news));
    }

    public NoticiaDTO salvarParaDepois(Long id) {
        News news = newsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Noticia nao encontrada: " + id));
        news.setLerDepois(news.getLerDepois() + 1);
        return toDTO(newsRepository.save(news));
    }

    private String encode(String s) {
        return s.replace(" ", "%20");
    }
}
