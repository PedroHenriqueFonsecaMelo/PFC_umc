package umc.exs.repository.foundation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.EmailVerificacao;

/** Gerencia os tokens de verificação de e-mail no banco de dados. */
@Repository
public interface EmailVerificacaoRepository extends JpaRepository<EmailVerificacao, Long> {

    /** Busca o token de verificação pelo valor do token enviado por e-mail ao cliente. */
    Optional<EmailVerificacao> findByToken(String token);

    /** Busca o token de verificação vinculado a um cliente específico. */
    Optional<EmailVerificacao> findByClienteId(Long clienteId);

    /** Remove o token de verificação do cliente, usado na remoção de conta. */
    void deleteByClienteId(Long clienteId);
}
