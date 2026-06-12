package umc.exs.model.entidades.logic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Representa um administrador da plataforma com acesso ao painel admin,
 * autenticado via JWT com role ADMIN.
 */
@Entity
@Table(name = "admins")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Administrador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nome do administrador exibido no painel.
    private String nome;
    // E-mail usado para login no painel admin.
    private String email;
    // Senha criptografada com BCrypt.
    private String password;
}
