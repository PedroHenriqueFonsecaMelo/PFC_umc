package umc.exs.repository.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;

/**
 * Repositório responsável por gerenciar os tokens de recuperação de senha no banco de dados,
 * permitindo busca e remoção de registros vinculados a clientes durante o fluxo de redefinição
 * de senha.
 */
public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    /**
     * Busca o registro de recuperação de senha pelo token enviado por e-mail ao cliente.
     */
    Optional<RecuperacaoSenha> findByToken(String token);

    /**
     * Busca o token de recuperação de senha vinculado a um cliente específico.
     */
    Optional<RecuperacaoSenha> findByCliente(Cliente cliente);

    /**
     * Remove o token de recuperação de senha do cliente após uso ou na remoção de conta.
     */
    void deleteByCliente(Cliente cliente);

}