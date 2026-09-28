package br.com.agora.api.controller.dto;

import java.util.List;

public record RecomendacaoDTO(List<NoticiaDTO> noticias, double beta,
                              int sinaisConsiderados, int sinaisPendentes) {
}
