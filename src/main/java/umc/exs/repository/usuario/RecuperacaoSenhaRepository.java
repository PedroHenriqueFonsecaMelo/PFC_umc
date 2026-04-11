package umc.exs.repository.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    Optional<RecuperacaoSenha> findByToken(String token);

    Optional<RecuperacaoSenha> findByCliente(Cliente cliente);

    void deleteByCliente(Cliente cliente);

}