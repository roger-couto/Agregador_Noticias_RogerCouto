package br.com.agora.api.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_feedback_eventos", indexes = {
        @Index(name = "idx_feedback_usuario_data", columnList = "usuario_id, criado_em"),
        @Index(name = "idx_feedback_news_data", columnList = "news_id, criado_em")
})
@Data
@NoArgsConstructor
public class FeedbackEvento {

    // Eu mantenho aqui cada ação, mesmo quando o estado atual do botão muda depois.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "cluster_id")
    // Registro o cluster conhecido naquele clique; pode estar vazio se ainda não calculei o grupo.
    private Long clusterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoFeedback tipo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public FeedbackEvento(Long usuarioId, Long newsId, Long clusterId, TipoFeedback tipo) {
        this.usuarioId = usuarioId;
        this.newsId = newsId;
        this.clusterId = clusterId;
        this.tipo = tipo;
    }
}
