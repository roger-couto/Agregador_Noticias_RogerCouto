package br.com.agora.api.domain.repository;

import br.com.agora.api.domain.model.NewsApiColetaEstado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsApiColetaEstadoRepository extends JpaRepository<NewsApiColetaEstado, String> {
}
