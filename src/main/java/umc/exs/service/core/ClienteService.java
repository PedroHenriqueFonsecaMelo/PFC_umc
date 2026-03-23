package umc.exs.service.core;

import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.ClienteMapper;
import umc.exs.mappers.EnderecoMapper;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.service.carteira.CarteiraService;
import umc.exs.service.senha.FieldValidation;
import umc.exs.service.senha.SenhaService;

@Service
@Slf4j
@RequiredArgsConstructor
public class ClienteService {
    
    // Repositories
    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EnderecoRepository enderecoRepository;

    // Serviços Especialistas
    private final ClientMainService coreService;
    private final CarteiraService carteiraService;
    private final SenhaService senhaService;
    private final EnderecoService enderecoService;
    private final CartaoService cartaoService;

    // Mappers e Segurança
    private final ClienteMapper clienteMapper;
    private final EnderecoMapper enderecoMapper;
    private final PasswordEncoder passwordEncoder;

    // ==========================================================
    // 🔹 CADASTRO E SALVAMENTO
    // ==========================================================

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        coreService.validarNovoCliente(signupDTO);

        Cliente cliente = clienteMapper.paraEntidade(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);

        return clienteMapper.paraDTO(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        coreService.validarNovoCliente(signupDTO);

        Cliente cliente = clienteMapper.paraEntidade(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0);

        // Delegação para serviços de infra
        Endereco endereco = enderecoService.saveOrReuseEndereco(enderecoDTO);
        cliente.getEnderecos().add(endereco);
        endereco.getClientes().add(cliente);

        Cartao cartao = cartaoService.saveOrReuseCartao(cartaoDTO);
        cliente.getCartoes().add(cartao);
        cartao.getClientes().add(cliente);

        return clienteMapper.paraDTO(clienteRepository.save(cliente));
    }

    // ==========================================================
    // 💾 ATUALIZAÇÃO E MANUTENÇÃO
    // ==========================================================

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO dto) {
        // Uso do coreService para busca e validação
        Cliente clienteExistente = coreService.buscarPorId(clienteId);
        coreService.validarAtualizacao(dto.getNome(), dto.getSenha());

        clienteExistente.setNome(FieldValidation.sanitize(dto.getNome()));
        clienteExistente.setDatanasc(dto.getDatanasc());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            clienteExistente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }
        atualizarEnderecosManualmente(clienteExistente, dto.getEnderecos());
        atualizarCartoesManualmente(clienteExistente, dto.getCartoes());

        return clienteMapper.paraDTO(clienteRepository.save(clienteExistente));
    }

    @Transactional
    public void deletarClientePorId(Long clienteId) {
        Cliente cliente = coreService.buscarPorId(clienteId);

        // Limpa associações M2M antes da remoção
        cliente.getEnderecos().forEach(e -> e.getClientes().remove(cliente));
        cliente.getCartoes().forEach(c -> c.getClientes().remove(cliente));

        clienteRepository.delete(cliente);
        log.info("Cliente ID {} removido com sucesso via Orquestrador.", clienteId);
    }

    // ==========================================================
    // 🪙 CARTEIRA E TRANSAÇÕES
    // ==========================================================

    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor, String metodo, String numCartao) {
        Cliente cliente = coreService.buscarPorId(clienteId);

        String finalCartao = (metodo.equalsIgnoreCase("CARTAO") && numCartao != null && numCartao.length() >= 4)
                ? numCartao.substring(numCartao.length() - 4)
                : "N/A";

        carteiraService.adicionarTokens(cliente, valor, metodo, finalCartao);
        return clienteMapper.paraDTO(cliente);
    }

    public List<Transacao> listarHistoricoTransacoes(Long clienteId) {
        return carteiraService.listarHistoricoPorCliente(clienteId);
    }

    // ==========================================================
    // 🔐 SEGURANÇA E BUSCA
    // ==========================================================

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        return coreService.encontrarPorEmail(email)
                .filter(c -> passwordEncoder.matches(senha, c.getSenha()))
                .map(clienteMapper::paraDTO);
                
    }

    public void iniciarRecuperacaoSenha(String email) {
        Cliente cliente = coreService.buscarPorEmail(email);
        senhaService.iniciarRecuperacao(cliente);
    }

    @Transactional
    public String alterarSenhaComToken(String token, String novaSenha) {
        senhaService.alterarSenhaComToken(token, novaSenha);
        return "Senha alterada com sucesso.";
    }

    public ClienteDTO buscarPorId(Long id) {
        return clienteMapper.paraDTO(coreService.buscarPorId(id));
    }

    // ==========================================================
    // 🛠 MÉTODOS PRIVADOS DE SINCRONIZAÇÃO
    // ==========================================================

    private void atualizarEnderecosManualmente(Cliente cliente, List<EnderecoDTO> dtos) {
        if (dtos == null)
            return;

        Set<Long> idsNoDto = new HashSet<>();
        for (EnderecoDTO dto : dtos) {
            if (dto.getId() != null && dto.getId() != 0)
                idsNoDto.add(dto.getId());
        }

        Iterator<Endereco> iter = cliente.getEnderecos().iterator();
        while (iter.hasNext()) {
            Endereco e = iter.next();
            if (!idsNoDto.contains(e.getId())) {
                e.getClientes().remove(cliente);
                iter.remove();
            }
        }

        for (EnderecoDTO endDto : dtos) {
            Endereco endereco;
            if (endDto.getId() != null && endDto.getId() != 0) {
                endereco = enderecoRepository.findById(endDto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Endereço não encontrado ID: " + endDto.getId()));
                enderecoMapper.atualizarEntidadeDeDto(endDto, endereco);
            } else {
                endereco = enderecoService.saveOrReuseEndereco(endDto);
                if (!endereco.getClientes().contains(cliente)) {
                    endereco.getClientes().add(cliente);
                }
            }
            if (!cliente.getEnderecos().contains(endereco)) {
                cliente.getEnderecos().add(endereco);
            }
        }
    }

    private void atualizarCartoesManualmente(Cliente cliente, List<CartaoDTO> dtos) {
        if (dtos == null)
            return;

        Set<Long> idsNoDto = new HashSet<>();
        for (CartaoDTO dto : dtos) {
            if (dto.getId() != null && dto.getId() != 0)
                idsNoDto.add(dto.getId());
        }

        Iterator<Cartao> iter = cliente.getCartoes().iterator();
        while (iter.hasNext()) {
            Cartao c = iter.next();
            if (!idsNoDto.contains(c.getId())) {
                c.getClientes().remove(cliente);
                iter.remove();
            }
        }
        
        for (CartaoDTO cartaoDto : dtos) {
            Cartao cartao;
            if (cartaoDto.getId() != null && cartaoDto.getId() != 0) {
                cartao = cartaoRepository.findById(cartaoDto.getId())
                        .orElseThrow(
                                () -> new IllegalArgumentException("Cartão não encontrado ID: " + cartaoDto.getId()));
            } else {
                cartao = cartaoService.saveOrReuseCartao(cartaoDto);
                if (!cartao.getClientes().contains(cliente)) {
                    cartao.getClientes().add(cliente);
                }
            }
            if (!cliente.getCartoes().contains(cartao)) {
                cliente.getCartoes().add(cartao);
            }
        }
    }

    public Optional<Cliente> buscarEntidadePorEmail(String email) {
        return coreService.encontrarPorEmail(email);
    }


    // Métodos para o fluxo de PIX delegando para a CarteiraService
    @Transactional
    public void registrarTransacaoPendente(Long clienteId, Double valor, String pagamentoId) {
        Cliente cliente = coreService.buscarPorId(clienteId);
        carteiraService.registrarIntencaoPagamento(cliente, valor, pagamentoId);
    }

    public boolean verificarSeFoiPago(String pagamentoId) {
        return carteiraService.verificarStatusPagamento(pagamentoId);
    }

    @Transactional
    public void aprovarPagamento(String pagamentoId) {
        carteiraService.confirmarPagamentoPix(pagamentoId);
    }

    public List<Transacao> listarHistoricoTransacoes(String email) {
        Cliente cliente = coreService.buscarPorEmail(email);
        return carteiraService.listarHistoricoPorCliente(cliente.getId());
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        log.debug("Orquestrando busca de cliente por e-mail: {}", email);
        
        return coreService.encontrarPorEmail(email)
                .map(clienteMapper::paraDTO);
    }

    @Transactional
    public boolean validarTokenRecuperacao(String token) {
        log.debug("Orquestrando validação de token de recuperação: {}", token);
        
        return senhaService.isTokenValido(token);
    }
}