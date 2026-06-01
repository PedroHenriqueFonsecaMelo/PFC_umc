package umc.exs.service.cliente.delegado;

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
import umc.exs.service.cliente.EnderecoService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteRepositoryService {

    private final ClienteRepository clienteRepository;
    private final RecuperacaoSenhaRepository tokenRepository;
    private final EnderecoService enderecoService;

    public Cliente buscarPorId(@NonNull Long id) {

        log.debug("Buscando cliente por ID={}", id);

        return clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cliente não encontrado ID={}", id);
                    return new IllegalArgumentException("Cliente não encontrado com ID: " + id);
                });
    }

    public Cliente buscarPorEmailOuFalhar(String email) {

        log.debug("Buscando cliente por email={}", email);

        return clienteRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> {
                    log.warn("Cliente não encontrado email={}", email);
                    return new IllegalArgumentException("Nenhum cliente vinculado ao e-mail: " + email);
                });
    }

    public Optional<Cliente> encontrarPorEmail(String email) {

        log.debug("Verificando existência de cliente email={}", email);

        return clienteRepository.findByEmailAndAtivoTrue(email);
    }

    public boolean existeEmailAtivo(String email) {

        boolean existe = clienteRepository.existsByEmailAndAtivoTrue(email);

        log.debug("Existe email ativo {} = {}", email, existe);

        return existe;
    }

    public boolean existeCpfAtivo(String cpf) {

        boolean existe = clienteRepository.existsByCpfAndAtivoTrue(cpf);

        log.debug("Existe CPF ativo {} = {}", cpf, existe);

        return existe;
    }

    public boolean encontrarPorCPF(String cpf) {

        boolean existe = clienteRepository.existsByCpf(cpf);

        log.debug("Existe CPF {} = {}", cpf, existe);

        return existe;
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {

        log.info("Salvando cliente email={} id={}", cliente.getEmail(), cliente.getId());

        Cliente salvo = clienteRepository.save(cliente);

        log.debug("Cliente salvo com sucesso id={}", salvo.getId());

        return salvo;
    }

    @Transactional
    public void deletarPorId(@NonNull Long id) {

        log.warn("Solicitado soft delete cliente id={}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Tentativa de deletar cliente inexistente id={}", id);
                    return new IllegalArgumentException("Tentativa de deletar cliente inexistente.");
                });

        cliente.setAtivo(false);
        cliente.setDeletedAt(LocalDateTime.now());

        clienteRepository.save(cliente);

        log.info("Soft delete aplicado cliente id={} email={}", id, cliente.getEmail());
    }

    @Transactional
    public void registrarFalhaLogin(Cliente cliente) {

        cliente.registrarFalhaLogin();
        clienteRepository.save(cliente);

        log.warn("Falha login cliente email={} tentativas={}", cliente.getEmail(), cliente.getTentativas());
    }

    @Transactional
    public void resetarTentativasLogin(Cliente cliente) {

        cliente.resetarTentativas();
        clienteRepository.save(cliente);

        log.info("Tentativas de login resetadas email={}", cliente.getEmail());
    }

    @Transactional
    public void excluirTokenRecuperacao(@NonNull RecuperacaoSenha registro) {

        tokenRepository.delete(registro);

        log.info("Token recuperação removido clienteId={}", registro.getCliente().getId());
    }

    @Transactional
    public void limparTokensAntigos(Cliente cliente) {

        tokenRepository.deleteByCliente(cliente);

        log.info("Tokens antigos removidos clienteId={}", cliente.getId());
    }

    @Transactional
    public void adicionarEnderecoParaUsuarioLogado(String email, Endereco dto) {

        log.info("Adicionando endereço para email={}", email);

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("Usuário não encontrado email={}", email);
                    return new RuntimeException("Usuário não encontrado");
                });

        enderecoService.vincularNovoEndereco(cliente, dto);

        Cliente salvo = clienteRepository.save(cliente);

        if (salvo.getEnderecoSelecionadoId() == null) {

            salvo.getEnderecos().stream()
                    .map(Endereco::getId)
                    .findFirst()
                    .ifPresent(id -> {
                        salvo.setEnderecoSelecionadoId(id);
                        clienteRepository.save(salvo);
                        log.info("Endereço padrão definido clienteId={} enderecoId={}", salvo.getId(), id);
                    });
        }

        log.info("Endereço vinculado com sucesso email={} id={}", email, cliente.getId());
    }

    @Transactional
    public void deletarEnderecoDoCliente(@NonNull Long clienteId, @NonNull Long enderecoId) {

        log.warn("Removendo endereço clienteId={} enderecoId={}", clienteId, enderecoId);

        Cliente cliente = buscarPorId(clienteId);

        enderecoService.deletarEnderecoDoCliente(cliente, enderecoId);

        if (enderecoId.equals(cliente.getEnderecoSelecionadoId())) {

            cliente.setEnderecoSelecionadoId(
                    cliente.getEnderecos().stream()
                            .map(Endereco::getId)
                            .findFirst()
                            .orElse(null));
        }

        clienteRepository.save(cliente);

        log.info("Endereço removido clienteId={} enderecoId={}", clienteId, enderecoId);
    }

    @Transactional
    public void atualizarEnderecoDoCliente(@NonNull Long clienteId, @NonNull Endereco dto) {

        log.info("Atualizando endereço clienteId={} enderecoId={}", clienteId, dto.getId());

        Cliente cliente = buscarPorId(clienteId);

        Long idAntigo = dto.getId();

        enderecoService.deletarEnderecoDoCliente(cliente, idAntigo);

        dto.setId(null);

        enderecoService.vincularNovoEndereco(cliente, dto);

        if (idAntigo.equals(cliente.getEnderecoSelecionadoId())) {

            cliente.getEnderecos().stream()
                    .map(Endereco::getId)
                    .findFirst()
                    .ifPresent(cliente::setEnderecoSelecionadoId);
        }

        clienteRepository.save(cliente);

        log.info("Endereço atualizado clienteId={}", clienteId);
    }

    @Transactional
    public void deletarCartaoDoCliente(@NonNull Long clienteId, @NonNull Long cartaoId) {

        log.warn("Removendo cartão clienteId={} cartaoId={}", clienteId, cartaoId);

        Cliente cliente = buscarPorId(clienteId);

        boolean removido = cliente.getCartoes()
                .removeIf(c -> c.getId().equals(cartaoId));

        if (!removido) {
            log.error("Cartão não encontrado clienteId={} cartaoId={}", clienteId, cartaoId);
            throw new IllegalArgumentException("Cartão não encontrado ou não pertence a este cliente.");
        }

        clienteRepository.save(cliente);

        log.info("Cartão removido clienteId={} cartaoId={}", clienteId, cartaoId);
    }

    public Cliente getByEmail(String email) {

        log.debug("getByEmail email={}", email);

        return clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }

    public Cliente getById(Long id) {

        log.debug("getById id={}", id);

        return clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }
}