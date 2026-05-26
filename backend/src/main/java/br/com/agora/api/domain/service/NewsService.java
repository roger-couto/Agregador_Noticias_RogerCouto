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

    @Value("${newsapi.key}")
    private String apiKey;

    @Value("${newsapi.base-url}")
    private String baseUrl;

    private final NewsRepository newsRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }
    private static List<NoticiaDTO> cacheRecentes = null;
    private static long cacheRecentesTimestamp = 0;
    private static final long CACHE_TTL_MS = 30 * 60 * 1000;

    private String resolverQuery(String tag) {
        return switch (tag.toLowerCase()) {
            case "tecnologia" -> "tecnologia OR inteligência artificial";
            case "economia"   -> "economia OR mercado financeiro";
            case "política"   -> "política brasileira";
            case "esporte"    -> "futebol OR esportes";
            case "saúde"      -> "saúde OR medicina";
            case "ciência"    -> "ciência OR pesquisa";
            case "cultura"    -> "cultura OR arte OR cinema";
            case "lazer"      -> "lazer OR entretenimento OR turismo";
            default           -> tag;
        };
    }

    public List<NoticiaDTO> buscarPorTag(String tag) {
        String query = resolverQuery(tag);
        String url = baseUrl + "/everything?q=" + encode(query)
                + "&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
        return fetchEConverter(url, tag, null);
    }

    public List<NoticiaDTO> buscarPorPortal(String portal) {
        String url;
        // Corrige o aviso removendo a substituição redundante de espaços
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

            // INTERNACIONAIS
            case "bbc news" ->
                    url = baseUrl + "/everything?sources=bbc-news&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "bloomberg" ->
                    url = baseUrl + "/everything?sources=bloomberg&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "cnn" ->
                    url = baseUrl + "/everything?sources=cnn&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "reuters" ->
                    url = baseUrl + "/everything?q=reuters&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "techcrunch" ->
                    url = baseUrl + "/everything?sources=techcrunch&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
            case "the new york times" ->
                    url = baseUrl + "/everything?q=\"new york times\"&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;

            default ->
                    url = baseUrl + "/everything?q=" + encode(portal) + "&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
        }

        return fetchEConverter(url, null, portal);
    }

    public List<NoticiaDTO> buscarRecentes() {
        long agora = System.currentTimeMillis();
        if (cacheRecentes != null && (agora - cacheRecentesTimestamp) < CACHE_TTL_MS) {
            System.out.println("[NewsService] Cache ativo, próxima atualização em "
                    + ((CACHE_TTL_MS - (agora - cacheRecentesTimestamp)) / 60000) + " min");
            return cacheRecentes;
        }
        String url = baseUrl + "/everything?q=brasil&language=pt&sortBy=publishedAt&pageSize=60&apiKey=" + apiKey;
        List<NoticiaDTO> resultado = fetchEConverter(url, null, null);
        if (!resultado.isEmpty()) {
            cacheRecentes = resultado;
            cacheRecentesTimestamp = agora;
            System.out.println("[NewsService] Cache de recentes updated");
        }
        return (resultado.isEmpty() && cacheRecentes != null) ? cacheRecentes : resultado;
    }

    private List<NoticiaDTO> fetchEConverter(String url, String tag, String portal) {
        List<NoticiaDTO> resultado = new ArrayList<>();
        try {
            String json = restTemplate.getForObject(url, String.class);
            JsonNode root = mapper.readTree(json);
            JsonNode articles = root.path("articles");

            for (JsonNode node : articles) {
                String titulo = node.path("title").asText();
                String urlNoticia = node.path("url").asText();
                if (titulo.equals("[Removed]") || titulo.isBlank() || urlNoticia.isBlank()) continue;

                News news = newsRepository.findByUrl(urlNoticia).orElseGet(() -> {
                    News nova = new News();
                    nova.setTitulo(titulo);
                    nova.setDescricao(node.path("description").asText());
                    nova.setUrl(urlNoticia);
                    nova.setImageUrl(node.path("urlToImage").asText());
                    nova.setPortal(portal != null ? portal : node.path("source").path("name").asText());
                    nova.setTag(tag);

                    String publishedAt = node.path("publishedAt").asText();
                    if (!publishedAt.isBlank()) {
                        nova.setPublicadoEm(OffsetDateTime.parse(publishedAt).toLocalDateTime());
                    } else {
                        nova.setPublicadoEm(LocalDateTime.now());
                    }
                    return newsRepository.save(nova);
                });

                resultado.add(toDTO(news));
            }
        } catch (Exception e) {
            System.err.println("[NewsService] Erro ao buscar noticias: " + e.getMessage());
        }
        return resultado;
    }

    public NoticiaDTO toDTO(News news) {
        return new NoticiaDTO(
                news.getId(), news.getTitulo(), news.getDescricao(),
                news.getUrl(), news.getImageUrl(), news.getPortal(),
                news.getTag(), news.getPublicadoEm(), news.getGostei(), news.getLerDepois()
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