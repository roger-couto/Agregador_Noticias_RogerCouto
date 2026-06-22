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
                .findByUsuarioIdAndNewsId(usuarioId, newsId) //Pergunta pro Banco se ja existe a interação
                .orElseGet(() -> {                           //deste usuario + essa noticia
                    Interacao nova = new Interacao();
                    nova.setUsuarioId(usuarioId);
                    nova.setNewsId(newsId);
                    return nova;
                });

        i.setCurtido(!i.isCurtido()); //inverte o boolean. false vira true e vice-versa

        newsRepository.findById(newsId).ifPresent(news -> { //contador de vezes que salvaram essa noticia
            news.setGostei(Math.max(0, news.getGostei() + (i.isCurtido() ? 1 : -1)));
            newsRepository.save(news);
        });
        return interacaoRepository.save(i); //salva e retorna o obj para o frontend
    }

    @Transactional
    public Interacao salvar(Long usuarioId, Long newsId) {
        Interacao i = interacaoRepository
                .findByUsuarioIdAndNewsId(usuarioId, newsId) //Pergunta pro Banco se ja existe a interação
                .orElseGet(() -> {                           //deste usuario + essa noticia
                    Interacao nova = new Interacao();
                    nova.setUsuarioId(usuarioId);
                    nova.setNewsId(newsId);
                    return nova;
                });
        i.setSalvo(!i.isSalvo()); //inverte o boolean. false vira true e vice-versa

        newsRepository.findById(newsId).ifPresent(news -> { //contador de vezes que salvaram essa noticia
            news.setLerDepois(Math.max(0, news.getLerDepois() + (i.isSalvo() ? 1 : -1)));
            newsRepository.save(news);
        });
        return interacaoRepository.save(i); //salva e retorna o obj para o frontend
    }

    public List<Interacao> listarTodas(Long usuarioId) {
        return interacaoRepository.findByUsuarioId(usuarioId)
                .stream()
                .filter(i -> i.isCurtido() || i.isSalvo())
                .collect(java.util.stream.Collectors.toList());
    }
}