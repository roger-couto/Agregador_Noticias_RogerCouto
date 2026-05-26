package br.com.agora.api.controller.dto;

public class AuthResponseDTO {
    private String token;
    private String nome;
    private String email;
    private Long usuarioId;

    public AuthResponseDTO(String token, String nome, String email, Long usuarioId) {
        this.token = token;
        this.nome = nome;
        this.email = email;
        this.usuarioId = usuarioId;
    }

    public String getToken() { return token; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public Long getUsuarioId() { return usuarioId; }
}
