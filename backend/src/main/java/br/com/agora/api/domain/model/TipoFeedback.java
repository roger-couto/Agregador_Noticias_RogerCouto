package br.com.agora.api.domain.model;

/** Ações explícitas e aberturas registradas no histórico de recomendação. */
public enum TipoFeedback {
    // Ativações e seus eventos de desfazer.
    LIKE,
    UNLIKE,
    SAVE,
    UNSAVE,
    // Preferência de assunto e respectiva remoção.
    MORE,
    LESS,
    CLEAR_MORE,
    CLEAR_LESS,
    OPEN_ARTICLE
}
