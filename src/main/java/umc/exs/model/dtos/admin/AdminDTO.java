package umc.exs.model.dtos.admin;

import umc.exs.model.entidades.foundation.Administrador;

public class AdminDTO {
    private Long id;
    private String nome;
    private String email;
    private String password;

    public AdminDTO() {
    }

    public static AdminDTO fromEntity(Administrador a) {
        if (a == null)
            return null;
        AdminDTO dto = new AdminDTO();
        dto.id = a.getId();
        dto.nome = a.getNome();
        dto.email = a.getEmail();
        return dto;
    }

    public Administrador toEntity() {
        Administrador a = new Administrador();
        a.setId(this.id);
        a.setNome(this.nome);
        a.setEmail(this.email);
        a.setPassword(this.password);
        return a;
    }

    // getters/setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
