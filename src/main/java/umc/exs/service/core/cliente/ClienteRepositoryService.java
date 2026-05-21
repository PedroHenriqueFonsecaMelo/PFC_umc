package umc.exs.service.core.cliente;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteRepositoryService {

    private final ClienteRepository clienteRepository;
    private final RecuperacaoSenhaRepository tokenRepository;
    private final EnderecoService enderecoService;

    // =====================================================
    // BUSCAS
    // =====================================================

    public Cliente buscarPorId(@NonNull Long id) {

        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cliente não encontrado com ID: " + id));
    }

    public Cliente buscarPorEmailOuFalhar(String email) {

        return clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum cliente vinculado ao e-mail: " + email));
    }

    public Optional<Cliente> encontrarPorEmail(String email) {

        return clienteRepository.findByEmailAndAtivoTrue(email);
    }

    public boolean existeEmailAtivo(String email) {

        return clienteRepository.existsByEmailAndAtivoTrue(email);
    }

    public boolean existeCpfAtivo(String cpf) {

        return clienteRepository.existsByCpfAndAtivoTrue(cpf);
    }

    public boolean encontrarPorCPF(String cpf) {

        return clienteRepository.existsByCpf(cpf);
    }

    // =====================================================
    // PERSISTÊNCIA
    // =====================================================

    @Transactional
    public Cliente salvar(Cliente cliente) {

        log.debug("Persistindo cliente: {}", cliente.getEmail());

        return clienteRepository.save(cliente);
    }

    @Transactional
    public void deletarPorId(@NonNull Long id) {

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tentativa de deletar cliente inexistente."));

        // Soft delete
        cliente.setAtivo(false);
        cliente.setDeletedAt(LocalDateTime.now());

        clienteRepository.save(cliente);

        log.info("Soft delete aplicado ao cliente ID {}.", id);
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @Transactional
    public void registrarFalhaLogin(Cliente cliente) {

        cliente.registrarFalhaLogin();

        clienteRepository.save(cliente);

        log.warn("Falha de login registrada para: {}", cliente.getEmail());
    }

    @Transactional
    public void resetarTentativasLogin(Cliente cliente) {

        cliente.resetarTentativas();

        clienteRepository.save(cliente);
    }

    // =====================================================
    // TOKENS
    // =====================================================

    @Transactional
    public void excluirTokenRecuperacao(
            @NonNull RecuperacaoSenha registro) {

        tokenRepository.delete(registro);

        log.info("Token de recuperação removido com sucesso.");
    }

    @Transactional
    public void limparTokensAntigos(Cliente cliente) {

        tokenRepository.deleteByCliente(cliente);
    }

    // =====================================================
    // ENDEREÇOS
    // =====================================================

    /**
     * Adiciona endereço ao cliente.
     */
    @Transactional
    public void adicionarEnderecoParaUsuarioLogado(
            String email,
            Endereco dto) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        enderecoService.vincularNovoEndereco(cliente, dto);

        Cliente salvo = clienteRepository.save(cliente);

        /**
         * Auto seleção:
         * seleciona o primeiro endereço caso
         * cliente ainda não possua seleção.
         */
        if (salvo.getEnderecoSelecionadoId() == null) {

            salvo.getEnderecos().stream()
                    .map(Endereco::getId)
                    .findFirst()
                    .ifPresent(id -> {

                        salvo.setEnderecoSelecionadoId(id);

                        clienteRepository.save(salvo);
                    });
        }

        log.info("Endereço vinculado ao cliente {}", email);
    }

    /**
     * Remove vínculo cliente-endereço.
     */
    @Transactional
    public void deletarEnderecoDoCliente(
            @NonNull Long clienteId,
            @NonNull Long enderecoId) {

        Cliente cliente = buscarPorId(clienteId);

        enderecoService.deletarEnderecoDoCliente(
                cliente,
                enderecoId);

        /**
         * Limpa seleção caso endereço removido
         * fosse o endereço selecionado.
         */
        if (enderecoId.equals(cliente.getEnderecoSelecionadoId())) {

            cliente.setEnderecoSelecionadoId(
                    cliente.getEnderecos().stream()
                            .map(Endereco::getId)
                            .findFirst()
                            .orElse(null));
        }

        clienteRepository.save(cliente);

        log.info(
                "Endereço ID {} removido do cliente {}",
                enderecoId,
                cliente.getEmail());
    }

    /**
     * Atualiza endereço do cliente.
     *
     * IMPORTANTE:
     * Como o relacionamento é ManyToMany,
     * o correto é:
     *
     * 1. remover vínculo antigo
     * 2. criar/reutilizar novo endereço
     * 3. vincular novo endereço
     *
     * Isso evita alterar endereço
     * compartilhado entre clientes.
     */
    @Transactional
    public void atualizarEnderecoDoCliente(
            @NonNull Long clienteId,
            @NonNull Endereco dto) {

        Cliente cliente = buscarPorId(clienteId);

        /**
         * Remove vínculo antigo.
         */
        enderecoService.deletarEnderecoDoCliente(
                cliente,
                dto.getId());

        /**
         * Cria/reutiliza novo endereço.
         */
        enderecoService.vincularNovoEndereco(
                cliente,
                dto);

        /**
         * Atualiza endereço selecionado.
         */
        if (dto.getId().equals(cliente.getEnderecoSelecionadoId())) {

            cliente.getEnderecos().stream()
                    .map(Endereco::getId)
                    .findFirst()
                    .ifPresent(cliente::setEnderecoSelecionadoId);
        }

        clienteRepository.save(cliente);

        log.info(
                "Endereço atualizado para cliente {}",
                cliente.getEmail());
    }

    // =====================================================
    // CARTÕES
    // =====================================================

    @Transactional
    public void deletarCartaoDoCliente(
            @NonNull Long clienteId,
            @NonNull Long cartaoId) {

        Cliente cliente = buscarPorId(clienteId);

        boolean removido = cliente.getCartoes()
                .removeIf(c -> c.getId().equals(cartaoId));

        if (!removido) {

            throw new IllegalArgumentException(
                    "Cartão não encontrado ou não pertence a este cliente.");
        }

        clienteRepository.save(cliente);

        log.info(
                "Cartão ID {} removido do cliente {}",
                cartaoId,
                cliente.getEmail());
    }
}