package umc.exs.model.DTO.user;

import umc.exs.model.entidades.Endereco;

public class EnderecoDTO {
    private Long id;
    private String pais;
    private String cep;
    private String estado;
    private String cidade;
    private String rua;
    private String bairro;
    private String numero;
    private String complemento;
    private String tipoResidencia;

    public EnderecoDTO() {
    }

    public static EnderecoDTO fromEntity(Endereco e) {
        if (e == null)
            return null;
        EnderecoDTO dto = new EnderecoDTO();
        dto.id = e.getId();
        dto.pais = e.getPais();
        dto.cep = e.getCep();
        dto.estado = e.getEstado();
        dto.cidade = e.getCidade();
        dto.rua = e.getRua();
        dto.bairro = e.getBairro();
        dto.numero = String.valueOf(e.getNumero());
        dto.complemento = e.getComplemento();
        dto.tipoResidencia = e.getTipoResidencia();
        return dto;
    }

    public Endereco toEntity() {
        Endereco e = new Endereco();
        e.setId(this.id);
        e.setPais(this.pais);
        e.setCep(this.cep);
        e.setEstado(this.estado);
        e.setCidade(this.cidade);
        e.setRua(this.rua);
        e.setBairro(this.bairro);
        if (this.numero != null)
            e.setNumero(this.numero);
        e.setComplemento(this.complemento);
        e.setTipoResidencia(this.tipoResidencia);
        return e;
    }

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getRua() {
        return rua;
    }

    public void setRua(String rua) {
        this.rua = rua;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getTipoResidencia() {
        return tipoResidencia;
    }

    public void setTipoResidencia(String tipoResidencia) {
        this.tipoResidencia = tipoResidencia;
    }
}
