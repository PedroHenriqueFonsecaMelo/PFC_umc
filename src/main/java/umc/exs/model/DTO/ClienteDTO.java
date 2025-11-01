package umc.exs.model.DTO;

import umc.exs.model.entidades.Cliente;

public class ClienteDTO {
    private Long id;
    private String nome;
    private String email;
    private String datanasc;
    private String gen;
    private String senha;

    public ClienteDTO() {
    }

    public ClienteDTO(Long id, String nome, String email, String senha, String datanasc, String gen) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.datanasc = datanasc;
        this.gen = gen;
        this.senha = senha;
    }

    public static ClienteDTO fromEntity(Cliente c) {
        if (c == null)
            return null;
        ClienteDTO dto = new ClienteDTO();
        dto.id = c.getId();
        dto.nome = c.getNome();
        dto.email = c.getEmail();
        dto.datanasc = c.getDatanasc();
        dto.gen = c.getGen();
        dto.senha = c.getSenha();
        return dto;
    }

    public Cliente toEntity() {
        Cliente c = new Cliente();
        c.setId(this.id);
        c.setNome(this.nome);
        c.setEmail(this.email);
        c.setDatanasc(this.datanasc);
        c.setGen(this.gen);
        c.setSenha(this.senha);
        return c;
    }

    // getters / setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDatanasc() {
        return datanasc;
    }

    public void setDatanasc(String datanasc) {
        this.datanasc = datanasc;
    }

    public String getGen() {
        return gen;
    }

    public void setGen(String gen) {
        this.gen = gen;
    }
    public String getSenha() {
        return senha;
    }
    public void setSenha(String senha) {
        this.senha = senha;
    }
}
