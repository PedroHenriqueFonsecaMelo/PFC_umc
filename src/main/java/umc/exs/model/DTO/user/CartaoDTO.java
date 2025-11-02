package umc.exs.model.DTO.user;

import umc.exs.model.entidades.Cartao;

public class CartaoDTO {
    private Long id;
    private String numero;
    private String bandeira;
    private String nomeTitular;
    private String validade;
    private String cvv;
    private boolean preferencial;

    public CartaoDTO() {
    }

    public static CartaoDTO fromEntity(Cartao c) {
        if (c == null)
            return null;
        CartaoDTO dto = new CartaoDTO();
        dto.id = c.getId();
        dto.numero = c.getNumero();
        dto.bandeira = c.getBandeira();
        dto.nomeTitular = c.getNomeTitular();
        dto.validade = c.getValidade();
        dto.cvv = c.getCvv();
        dto.preferencial = c.isPreferencial();
        return dto;
    }

    public Cartao toEntity() {
        Cartao c = new Cartao();
        c.setId(this.id);
        c.setNumero(this.numero);
        c.setBandeira(this.bandeira);
        c.setNomeTitular(this.nomeTitular);
        c.setValidade(this.validade);
        c.setCvv(this.cvv);
        c.setPreferencial(this.preferencial);
        return c;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBandeira() {
        return bandeira;
    }

    public void setBandeira(String bandeira) {
        this.bandeira = bandeira;
    }

    public String getNomeTitular() {
        return nomeTitular;
    }

    public void setNomeTitular(String nomeTitular) {
        this.nomeTitular = nomeTitular;
    }

    public String getValidade() {
        return validade;
    }

    public void setValidade(String validade) {
        this.validade = validade;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    public boolean isPreferencial() {
        return preferencial;
    }

    public void setPreferencial(boolean preferencial) {
        this.preferencial = preferencial;
    }
}
