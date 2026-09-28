package br.com.agora.api.domain.service;

import br.com.agora.api.controller.dto.EmbeddingRequestDTO;
import br.com.agora.api.controller.dto.EmbeddingResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class EmbeddingClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedding.service.url:http://localhost:8001}")
    private String serviceUrl;

    public EmbeddingResponseDTO gerarEmbeddings(List<String> textos) {
        return restTemplate.postForObject(
                serviceUrl + "/embeddings",
                new EmbeddingRequestDTO(textos),
                EmbeddingResponseDTO.class
        );
    }
}
