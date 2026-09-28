package br.com.agora.api.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_cluster_centroides", uniqueConstraints =
        @UniqueConstraint(columnNames = {"versao_modelo", "ordem_cluster"}))
@Data
@NoArgsConstructor
public class ClusterCentroide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "versao_modelo", nullable = false)
    private Integer versaoModelo;

    @Column(name = "ordem_cluster", nullable = false)
    private Integer ordemCluster;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "centroide", columnDefinition = "jsonb", nullable = false)
    private List<Double> centroide;

    @Column(name = "quantidade_treino", nullable = false)
    private Integer quantidadeTreino;

    @Column(name = "embedding_model", nullable = false, length = 160)
    private String embeddingModel;

    @Column(name = "embedding_revision", nullable = false, length = 80)
    private String embeddingRevision;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
