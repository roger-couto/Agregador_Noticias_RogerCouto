package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.FeedbackEvento;
import br.com.agora.api.domain.model.TipoFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackEventoRepository extends JpaRepository<FeedbackEvento, Long> {
    List<FeedbackEvento> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    boolean existsByUsuarioIdAndNewsIdAndTipoAndCriadoEmAfter(
            Long usuarioId, Long newsId, TipoFeedback tipo, LocalDateTime instante);
    // O recomendador limita-se aos sinais mais recentes para manter consulta e cálculo leves.
    List<FeedbackEvento> findTop500ByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}
