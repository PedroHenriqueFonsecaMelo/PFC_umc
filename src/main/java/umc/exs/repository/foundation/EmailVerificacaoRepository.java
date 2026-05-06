package umc.exs.repository.foundation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import umc.exs.model.entidades.foundation.EmailVerificacao;

@Repository
public interface EmailVerificacaoRepository extends JpaRepository<EmailVerificacao, Long> {

    Optional<EmailVerificacao> findByToken(String token);

    Optional<EmailVerificacao> findByClienteId(Long clienteId);

    void deleteByClienteId(Long clienteId);
}
