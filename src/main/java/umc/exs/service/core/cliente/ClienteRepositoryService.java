package umc.exs.service.core.cliente;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.EnderecoMapper;
import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.EnderecoRepository;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteRepositoryService {

    private final ClienteRepository clienteRepository;
    private final RecuperacaoSenhaRepository tokenRepository;
    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    // --- Buscas ---

    public Cliente buscarPorId(@NonNull Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com ID: " + id));
    }

    public Cliente buscarPorEmailOuFalhar(String email) {
        return clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum cliente vinculado ao e-mail: " + email));
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

    // --- Persistência ---

    @Transactional
    public Cliente salvar(Cliente cliente) {
        log.debug("Persistindo cliente: {}", cliente.getEmail());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void deletarPorId(@NonNull Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tentativa de deletar cliente inexistente."));
        // Soft delete — nunca remove fisicamente para preservar dados financeiros (LGPD)
        cliente.setAtivo(false);
        cliente.setDeletedAt(LocalDateTime.now());
        clienteRepository.save(cliente);
        log.info("Soft delete aplicado ao cliente ID {}.", id);
    }

    // --- Gerenciamento de Estado de Segurança (Movido do Domain) ---

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

    // --- Gerenciamento de Tokens de Recuperação ---

    @Transactional
    public void excluirTokenRecuperacao(@NonNull RecuperacaoSenha registro) {
        tokenRepository.delete(registro);
        log.info("Token de recuperação removido com sucesso.");
    }

    @Transactional
    public void limparTokensAntigos(Cliente cliente) {
        tokenRepository.deleteByCliente(cliente);
    }

    @Transactional
    public void adicionarEnderecoParaUsuarioLogado(String email, EnderecoDTO enderecoDTO) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        Endereco novoEndereco = enderecoMapper.paraEntidade(enderecoDTO);

        cliente.getEnderecos().add(novoEndereco);

        clienteRepository.save(cliente);
    }

    @Transactional
    public void deletarEnderecoDoCliente(@NonNull Long clienteId, @NonNull Long enderecoId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com ID: " + clienteId));

        boolean removido = cliente.getEnderecos().removeIf(e -> e.getId().equals(enderecoId));

        if (!removido) {
            throw new IllegalArgumentException("Endereço não encontrado ou não pertence a este cliente.");
        }

        clienteRepository.save(cliente);
        log.info("Endereço ID {} removido do cliente {}", enderecoId, cliente.getEmail());
    }

    @Transactional
    public void deletarCartaoDoCliente(@NonNull Long clienteId, @NonNull Long cartaoId) {

        Cliente cliente = buscarPorId(clienteId);

        boolean removido = cliente.getCartoes().removeIf(c -> c.getId().equals(cartaoId));

        if (!removido) {
            throw new IllegalArgumentException("Cartão não encontrado ou não pertence a este cliente.");
        }

        clienteRepository.save(cliente);
        log.info("Cartão ID {} removido com sucesso do cliente {}", cartaoId, cliente.getEmail());
    }

    @Transactional
    public void atualizarEnderecoDoCliente(@NonNull Long clienteId, @NonNull EnderecoDTO dto) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com ID: " + clienteId));

        boolean pertence = cliente.getEnderecos().stream()
                .anyMatch(e -> e.getId().equals(dto.getId()));

        if (!pertence) {
            throw new IllegalArgumentException("Endereço não pertence a este cliente.");
        }

        Endereco endereco = enderecoRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado: " + dto.getId()));

        enderecoMapper.atualizarEntidadeDeDto(dto, endereco);
        enderecoRepository.save(endereco);
        log.info("Endereço ID {} atualizado para o cliente {}", dto.getId(), cliente.getEmail());
    }
}