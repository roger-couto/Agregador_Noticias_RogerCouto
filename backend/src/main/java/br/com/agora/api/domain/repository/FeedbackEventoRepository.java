package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.FeedbackEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackEventoRepository extends JpaRepository<FeedbackEvento, Long> {
    List<FeedbackEvento> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
    List<FeedbackEvento> findTop500ByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}
