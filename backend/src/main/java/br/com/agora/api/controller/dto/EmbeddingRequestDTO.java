package br.com.agora.api.controller.dto;

import java.util.List;

public record EmbeddingRequestDTO(List<String> texts) {
}
