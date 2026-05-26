package br.com.agora.api.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_news")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(length = 1000)
    private String url;

    @Column(length = 1000)
    private String imageUrl;

    private String portal;   // G1, BBC News, CNN, New York Times

    private String tag;      // Tecnologia, Economia, etc.

    private LocalDateTime publicadoEm;

    private int gostei = 0;

    private int lerDepois = 0;
}
