package br.com.agora.api.controller.dto;

import java.util.List;

public record EmbeddingResponseDTO(List<List<Double>> embeddings, String model, String revision) {
}
