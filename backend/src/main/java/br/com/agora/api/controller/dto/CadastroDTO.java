package br.com.agora.api.controller.dto;

public class CadastroDTO {
    private String nome;
    private String email;
    private String senha;
    private String regiao;

    public CadastroDTO() {}

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getRegiao() { return regiao; }
    public void setRegiao(String regiao) { this.regiao = regiao; }
}
