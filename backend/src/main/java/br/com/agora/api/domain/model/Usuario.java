package br.com.agora.api.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    private String regiao;

    // Tags de interesse para personalização
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_preferencias", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "tag")
    private Set<String> preferencias = new HashSet<>();
}