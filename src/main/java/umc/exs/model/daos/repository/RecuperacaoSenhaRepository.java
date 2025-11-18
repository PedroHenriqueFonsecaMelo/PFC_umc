package umc.exs.model.daos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import umc.exs.model.entidades.foundation.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {

    /**
     * Busca um token de recuperação pelo seu valor.
     */
    Optional<RecuperacaoSenha> findByToken(String token);

    /**
     * Busca um token ativo para um determinado cliente.
     */
    Optional<RecuperacaoSenha> findByCliente(Cliente cliente);
    
    /**
     * Deleta todos os tokens de recuperação associados a um cliente.
     */
    void deleteByCliente(Cliente cliente);
}