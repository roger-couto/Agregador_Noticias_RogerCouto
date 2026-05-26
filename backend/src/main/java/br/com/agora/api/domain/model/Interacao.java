package br.com.agora.api.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tb_interacoes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "news_id"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(nullable = false)
    private boolean curtido = false;

    @Column(nullable = false)
    private boolean salvo = false;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();
}