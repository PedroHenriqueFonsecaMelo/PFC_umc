package umc.exs.repository.logic;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.logic.Administrador;

import java.util.Optional;

/**
 * Gerencia os administradores no banco de dados, usado na autenticação e
 * validação do painel admin.
 */
public interface AdminRepository extends JpaRepository<Administrador, Long> {

    /** Busca um admin pelo e-mail para autenticação no painel administrativo. */
    Optional<Administrador> findByEmail(String email);

    /** Busca um admin validando simultaneamente e-mail e ID para segurança adicional. */
    public Optional<Administrador> findByEmailAndId(String email, Long id);
}
