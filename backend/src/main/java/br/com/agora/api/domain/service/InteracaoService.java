package br.com.agora.api.domain.service;

import br.com.agora.api.domain.model.FeedbackEvento;
import br.com.agora.api.domain.model.Interacao;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.model.TipoFeedback;
import br.com.agora.api.domain.repository.FeedbackEventoRepository;
import br.com.agora.api.domain.repository.InteracaoRepository;
import br.com.agora.api.domain.repository.NewsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InteracaoService {

    private final InteracaoRepository interacaoRepository;
    private final FeedbackEventoRepository feedbackEventoRepository;
    private final NewsRepository newsRepository;

    public InteracaoService(InteracaoRepository interacaoRepository,
                            FeedbackEventoRepository feedbackEventoRepository,
                            NewsRepository newsRepository) {
        this.interacaoRepository = interacaoRepository;
        this.feedbackEventoRepository = feedbackEventoRepository;
        this.newsRepository = newsRepository;
    }

    @Transactional
    public Interacao registrarFeedback(Long usuarioId, Long newsId, TipoFeedback tipo) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notícia não encontrada."));

        Interacao interacao = interacaoRepository.findByUsuarioIdAndNewsId(usuarioId, newsId)
                .orElseGet(() -> {
                    Interacao nova = new Interacao();
                    nova.setUsuarioId(usuarioId);
                    nova.setNewsId(newsId);
                    return nova;
                });

        boolean curtidoAntes = interacao.isCurtido();
        boolean salvoAntes = interacao.isSalvo();
        boolean mudouEstado = aplicarEstado(interacao, tipo);

        if (mudouEstado) {
            interacao.setAtualizadoEm(LocalDateTime.now());
            atualizarContadoresLegados(news, curtidoAntes, salvoAntes, interacao);
        }

        Interacao interacaoSalva = interacaoRepository.save(interacao);
        feedbackEventoRepository.save(new FeedbackEvento(usuarioId, newsId, news.getClusterId(), tipo));
        return interacaoSalva;
    }

    private boolean aplicarEstado(Interacao interacao, TipoFeedback tipo) {
        return switch (tipo) {
            case LIKE -> definir(interacao.isCurtido(), true, interacao::setCurtido);
            case UNLIKE -> definir(interacao.isCurtido(), false, interacao::setCurtido);
            case SAVE -> definir(interacao.isSalvo(), true, interacao::setSalvo);
            case UNSAVE -> definir(interacao.isSalvo(), false, interacao::setSalvo);
            case MORE -> {
                boolean mudou = !interacao.isVerMais() || interacao.isVerMenos();
                interacao.setVerMais(true);
                interacao.setVerMenos(false);
                yield mudou;
            }
            case LESS -> {
                boolean mudou = !interacao.isVerMenos() || interacao.isVerMais();
                interacao.setVerMenos(true);
                interacao.setVerMais(false);
                yield mudou;
            }
            case CLEAR_MORE -> definir(interacao.isVerMais(), false, interacao::setVerMais);
            case CLEAR_LESS -> definir(interacao.isVerMenos(), false, interacao::setVerMenos);
            case OPEN_ARTICLE -> false;
        };
    }

    private void atualizarContadoresLegados(News news, boolean curtidoAntes, boolean salvoAntes,
                                             Interacao interacao) {
        if (curtidoAntes != interacao.isCurtido()) {
            news.setGostei(Math.max(0, news.getGostei() + (interacao.isCurtido() ? 1 : -1)));
        }
        if (salvoAntes != interacao.isSalvo()) {
            news.setLerDepois(Math.max(0, news.getLerDepois() + (interacao.isSalvo() ? 1 : -1)));
        }
        if (curtidoAntes != interacao.isCurtido() || salvoAntes != interacao.isSalvo()) {
            newsRepository.save(news);
        }
    }

    @Transactional
    public Interacao curtir(Long usuarioId, Long newsId) {
        boolean jaCurtido = interacaoRepository.findByUsuarioIdAndNewsId(usuarioId, newsId)
                .map(Interacao::isCurtido).orElse(false);
        return registrarFeedback(usuarioId, newsId, jaCurtido ? TipoFeedback.UNLIKE : TipoFeedback.LIKE);
    }

    @Transactional
    public Interacao salvar(Long usuarioId, Long newsId) {
        boolean jaSalvo = interacaoRepository.findByUsuarioIdAndNewsId(usuarioId, newsId)
                .map(Interacao::isSalvo).orElse(false);
        return registrarFeedback(usuarioId, newsId, jaSalvo ? TipoFeedback.UNSAVE : TipoFeedback.SAVE);
    }

    public List<Interacao> listarTodas(Long usuarioId) {
        return interacaoRepository.findByUsuarioId(usuarioId).stream()
                .filter(i -> i.isCurtido() || i.isSalvo() || i.isVerMais() || i.isVerMenos())
                .toList();
    }

    private interface BooleanSetter {
        void set(boolean value);
    }

    private boolean definir(boolean atual, boolean novo, BooleanSetter setter) {
        if (atual == novo) return false;
        setter.set(novo);
        return true;
    }
}
