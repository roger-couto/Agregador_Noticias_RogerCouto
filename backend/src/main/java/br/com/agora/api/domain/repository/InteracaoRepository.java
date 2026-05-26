package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.Interacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface InteracaoRepository extends JpaRepository<Interacao, Long> {

    Optional<Interacao> findByUsuarioIdAndNewsId(Long usuarioId, Long newsId);

    List<Interacao> findByUsuarioIdAndCurtidoTrue(Long usuarioId);

    List<Interacao> findByUsuarioIdAndSalvoTrue(Long usuarioId);

    List<Interacao> findByUsuarioId(Long usuarioId);
}