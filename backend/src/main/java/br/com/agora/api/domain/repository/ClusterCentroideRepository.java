package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.ClusterCentroide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterCentroideRepository extends JpaRepository<ClusterCentroide, Long> {
    List<ClusterCentroide> findByVersaoModeloOrderByOrdemClusterAsc(Integer versaoModelo);
}
