package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {


    Optional<News> findByUrl(String url);
    List<News> findByPortalIgnoreCaseOrderByPublicadoEmDesc(String portal);
    List<News> findAllByOrderByPublicadoEmDesc();
    List<News> findTop60ByOrderByPublicadoEmDesc();
    List<News> findTop60ByPortalIgnoreCaseOrderByPublicadoEmDesc(String portal);
    List<News> findAllByEmbeddingIsNotNullOrderByIdAsc();
    List<News> findTop1000ByEmbeddingIsNotNullOrderByPublicadoEmDesc();
    List<News> findTop100ByOrderByPublicadoEmDesc();
    List<News> findAllByEmbeddingIsNullAndTituloIsNotNullOrderByIdAsc();
}
