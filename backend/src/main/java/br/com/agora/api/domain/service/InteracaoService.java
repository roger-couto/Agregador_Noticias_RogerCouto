package br.com.agora.api.domain.service;

import br.com.agora.api.domain.model.Interacao;
import br.com.agora.api.domain.model.News;
import br.com.agora.api.domain.repository.InteracaoRepository;
import br.com.agora.api.domain.repository.NewsRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InteracaoService {

    @Autowired
    private InteracaoRepository interacaoRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Transactional
    public Interacao curtir(Long usuarioId, Long newsId) {
        Interacao i = interacaoRepository
                .findByUsuarioIdAndNewsId(usuarioId, newsId)
                .orElseGet(() -> {
                    Interacao nova = new Interacao();
                    nova.setUsuarioId(usuarioId);
                    nova.setNewsId(newsId);
                    return nova;
                });

        i.setCurtido(!i.isCurtido());

        newsRepository.findById(newsId).ifPresent(news -> {
            news.setGostei(Math.max(0, news.getGostei() + (i.isCurtido() ? 1 : -1)));
            newsRepository.save(news);
        });
        return interacaoRepository.save(i);
    }

    @Transactional
    public Interacao salvar(Long usuarioId, Long newsId) {
        Interacao i = interacaoRepository
                .findByUsuarioIdAndNewsId(usuarioId, newsId)
                .orElseGet(() -> {
                    Interacao nova = new Interacao();
                    nova.setUsuarioId(usuarioId);
                    nova.setNewsId(newsId);
                    return nova;
                });
        i.setSalvo(!i.isSalvo());

        newsRepository.findById(newsId).ifPresent(news -> {
            news.setLerDepois(Math.max(0, news.getLerDepois() + (i.isSalvo() ? 1 : -1)));
            newsRepository.save(news);
        });
        return interacaoRepository.save(i);
    }

    public List<Interacao> listarTodas(Long usuarioId) {
        return interacaoRepository.findByUsuarioId(usuarioId)
                .stream()
                .filter(i -> i.isCurtido() || i.isSalvo())
                .collect(java.util.stream.Collectors.toList());
    }
}