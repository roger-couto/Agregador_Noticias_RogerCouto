package br.com.agora.api.controller.dto;

import java.time.LocalDateTime;

public class NoticiaDTO {
    private Long id;
    private String titulo;
    private String descricao;
    private String url;
    private String imageUrl;
    private String portal;
    private String tag;
    private LocalDateTime publicadoEm;
    private int gostei;
    private int lerDepois;

    public NoticiaDTO() {}

    public NoticiaDTO(Long id, String titulo, String descricao, String url,
                      String imageUrl, String portal, String tag,
                      LocalDateTime publicadoEm, int gostei, int lerDepois) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.url = url;
        this.imageUrl = imageUrl;
        this.portal = portal;
        this.tag = tag;
        this.publicadoEm = publicadoEm;
        this.gostei = gostei;
        this.lerDepois = lerDepois;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getPortal() { return portal; }
    public void setPortal(String portal) { this.portal = portal; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public LocalDateTime getPublicadoEm() { return publicadoEm; }
    public void setPublicadoEm(LocalDateTime publicadoEm) { this.publicadoEm = publicadoEm; }
    public int getGostei() { return gostei; }
    public void setGostei(int gostei) { this.gostei = gostei; }
    public int getLerDepois() { return lerDepois; }
    public void setLerDepois(int lerDepois) { this.lerDepois = lerDepois; }
}
