package br.com.agora.api.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Controle persistente para limitar a frequência de cada consulta compartilhada. */
@Entity
@Table(name = "tb_newsapi_coleta_estado")
@Data
@NoArgsConstructor
public class NewsApiColetaEstado {
    @Id
    @Column(name = "chave_consulta", length = 100)
    private String chaveConsulta;

    @Column(name = "ultima_tentativa", nullable = false)
    private LocalDateTime ultimaTentativa;

    @Column(name = "ultima_coleta")
    private LocalDateTime ultimaColeta;

    @Column(name = "artigos_recebidos")
    private Integer artigosRecebidos;

    @Column(name = "requisicoes_no_dia")
    private Integer requisicoesNoDia;

    @Column(name = "ultimo_erro", length = 500)
    private String ultimoErro;
}
