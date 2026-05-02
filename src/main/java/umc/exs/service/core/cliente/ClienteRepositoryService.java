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
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteRepositoryService {

    private final ClienteRepository clienteRepository;
    private final RecuperacaoSenhaRepository tokenRepository;
    private final EnderecoMapper enderecoMapper;

    // --- Buscas ---

    public Cliente buscarPorId(@NonNull Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com ID: " + id));
    }

    public Cliente buscarPorEmailOuFalhar(String email) {
        return clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Nenhum cliente vinculado ao e-mail: " + email));
    }

    public Optional<Cliente> encontrarPorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    // --- Persistência ---

    @Transactional
    public Cliente salvar(Cliente cliente) {
        log.debug("Persistindo cliente: {}", cliente.getEmail());
        return clienteRepository.save(cliente);
    }

    @Transactional
    public void deletarPorId(@NonNull Long id) {
        if (!clienteRepository.existsById(id)) {
            throw new IllegalArgumentException("Tentativa de deletar cliente inexistente.");
        }
        clienteRepository.deleteById(id);
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
}