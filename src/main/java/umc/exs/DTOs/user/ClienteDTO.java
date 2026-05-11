package umc.exs.dtos.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
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
     * Data de nascimento (tipo correto).
     */
    private LocalDate datanasc;

    /**
     * Senha.
     */
    private String senha;

    /**
     * CPF.
     */
    private String cpf;

    /**
     * Saldo atual de tokens.
     */
    private Double saldoTokens = 0.0;

    /**
     * URL da foto de perfil.
     */
    private String fotoPerfil;

    /**
     * Data de criação da conta.
     */
    private LocalDateTime dataCriacao;

    /**
     * ID do endereço selecionado para entrega.
     */
    private Long enderecoSelecionadoId;

    /**
     * Lista de endereços.
     */
    private List<EnderecoDTO> enderecos = new ArrayList<>();

    /**
     * Lista de cartões.
     */
    private List<CartaoDTO> cartoes = new ArrayList<>();

    /**
     * Construtor para criação (sem ID e data automática).
     */
    public ClienteDTO(String nome, String email, LocalDate datanasc, String senha,
                      String cpf, String fotoPerfil,
                      List<EnderecoDTO> enderecos,
                      List<CartaoDTO> cartoes) {

        this.nome = nome;
        this.email = email;
        this.datanasc = datanasc;
        this.senha = senha;
        this.cpf = cpf;
        this.fotoPerfil = fotoPerfil;
        this.enderecos = enderecos;
        this.cartoes = cartoes;
    }
}