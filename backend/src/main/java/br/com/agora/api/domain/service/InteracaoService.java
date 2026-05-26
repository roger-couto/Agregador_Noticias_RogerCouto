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
        return interacaoRepository.save(i);
    }

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
        return interacaoRepository.save(i);
    }

    public List<Interacao> listarTodas(Long usuarioId) {
        return interacaoRepository.findByUsuarioId(usuarioId);
    }
}