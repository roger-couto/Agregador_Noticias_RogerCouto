package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {


    Optional<News> findByUrl(String url);
    List<News> findByTagIgnoreCaseOrderByPublicadoEmDesc(String tag);
    List<News> findByPortalIgnoreCaseOrderByPublicadoEmDesc(String portal);
    List<News> findAllByOrderByPublicadoEmDesc();
}
