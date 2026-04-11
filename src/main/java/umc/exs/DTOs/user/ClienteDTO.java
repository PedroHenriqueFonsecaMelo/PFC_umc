package umc.exs.DTOs.user;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "senha" })
public class ClienteDTO {

    /**
     * ID único do cliente no banco.
     */
    private Long id;

    /**
     * Nome completo do cliente.
     */
    private String nome;

    /**
     * E-mail único para login.
     */
    private String email;

    /**
     * Data nascimento formato string.
     */
    private String datanasc;

    /**
     * Gênero M/F/outros.
     */
    private String gen;

    /**
     * Senha criptografada (não exposta).
     */
    private String senha;

    /**
     * CPF para validação fiscal.
     */
    private String cpf;

    /**
     * Saldo atual tokens T$.
     */
    private Double saldoTokens = 0.0;

    /**
     * URL da foto de perfil do cliente.
     */
    private String fotoPerfil;

    /**
     * Lista endereços associados.
     */
    private List<EnderecoDTO> enderecos = new ArrayList<>();

    /**
     * Lista cartões cadastrados.
     */
    private List<CartaoDTO> cartoes = new ArrayList<>();
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * DTO transferência dados Cliente (perfil, listagem).
 * Lombok Data, ToString exclude senha.
 * Campos essenciais + listas endereços/cartões.
 * SaldoTokens default 0, usada frontend/backend.
 */
