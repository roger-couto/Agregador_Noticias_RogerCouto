package br.com.agora.api.domain.service;

import br.com.agora.api.controller.dto.NoticiaDTO;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.repository.NewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.Normalizer;

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

    public boolean isNewsApiConfigurada() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<NoticiaDTO> buscarPorPortal(String portal) {
        String chave = normalizarPortal(portal);
        return newsRepository.findAllByOrderByPublicadoEmDesc().stream()
                .filter(news -> normalizarPortal(news.getPortal()).equals(chave))
                .limit(60)
                .map(this::toDTO)
                .toList();
    }

    public List<NoticiaDTO> buscarRecentes() {
        return newsRepository.findTop60ByOrderByPublicadoEmDesc().stream().map(this::toDTO).toList();
    }

    /** Consulta a API usando a chave no header e importa resultados deduplicados por URL. */
    public int coletarDaNewsApi(String url, String portal) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("NEWSAPI_KEY não configurada");
        }
        List<News> resultado = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        JsonNode root = mapper.readTree(response.getBody());
        if (!"ok".equalsIgnoreCase(root.path("status").asText())) {
            throw new IllegalStateException("NewsAPI não confirmou a coleta: " + root.path("message").asText("erro sem detalhe"));
        }
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
                    newsRepository.save(existente);
                }
                if (portal != null && !portal.isBlank()) existente.setPortal(portal);
                return newsRepository.save(existente);
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
        return resultado.size();
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

    private String normalizarPortal(String portal) {
        if (portal == null) return "";
        return Normalizer.normalize(portal.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ");
    }
}
