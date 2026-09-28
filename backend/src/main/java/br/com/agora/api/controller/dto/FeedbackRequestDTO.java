package br.com.agora.api.controller.dto;

import br.com.agora.api.domain.model.TipoFeedback;
import jakarta.validation.constraints.NotNull;

public record FeedbackRequestDTO(@NotNull TipoFeedback tipo) {
}
