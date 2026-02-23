package umc.exs.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.daos.repository.TransacaoRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final CartaoRepository cartaoRepository;
    private final EnderecoRepository enderecoRepository;
    private final TransacaoRepository transacaoRepository; 
    private final PasswordEncoder passwordEncoder;
    private final EnderecoService enderecoService;
    private final CartaoService cartaoService;
    private final ClienteMapper clienteMapper;
    private final EnderecoMapper enderecoMapper;

    // --- MÉTODOS DE COMPRA E CARTEIRA ---

    /**
     * Verifica se o cliente possui algum cartão cadastrado no perfil.
     */
    public boolean possuiCartaoCadastrado(String email) {
        return clienteRepository.findByEmail(email)
                .map(c -> c.getCartoes() != null && !c.getCartoes().isEmpty())
                .orElse(false);
    }

    /**
     * Efetiva a soma dos tokens e registra a transação no histórico.
     */
    @Transactional
    public ClienteDTO adicionarTokens(Long clienteId, Double valor, String metodo, String numCartao) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // 1. Atualiza o saldo do cliente
        Double saldoAtual = cliente.getSaldoTokens();
        if (saldoAtual == null) {
            saldoAtual = 0.0;
        }
        cliente.setSaldoTokens(saldoAtual + valor);
        Cliente salvo = clienteRepository.save(cliente);

        // 2. Prepara o registro da transação para auditoria
        // Salvamos apenas os 4 últimos dígitos do cartão se for o método CARTAO
        String finalCartao = (metodo.equalsIgnoreCase("CARTAO") && numCartao != null && numCartao.length() >= 4)
                ? numCartao.substring(numCartao.length() - 4)
                : null;

        Transacao transacao = Transacao.builder()
                .cliente(cliente)
                .valor(valor)
                .dataHora(LocalDateTime.now())
                .metodoPagamento(metodo)
                .finalCartao(finalCartao)
                .build();

        transacaoRepository.save(transacao);

        return clienteMapper.toDTO(salvo);
    }

    // --- MÉTODOS DE BUSCA E ENTIDADE ---

    public Optional<Cliente> buscarEntidadePorEmail(String email) {
        return clienteRepository.findByEmail(email);
    }

    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return clienteRepository.findByEmail(email).map(clienteMapper::toDTO);
    }

    // --- MÉTODOS DE CADASTRO E ATUALIZAÇÃO ---

    @Transactional
    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        validarDadosSignup(signupDTO);
        Cliente cliente = clienteMapper.toEntity(signupDTO);
        cliente.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        cliente.setSaldoTokens(0.0); // Inicializa a carteira zerada
        Cliente salvo = clienteRepository.save(cliente);
        return clienteMapper.toDTO(salvo);
    }

    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO dto) {
        Cliente clienteExistente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

        clienteExistente.setNome(dto.getNome());
        clienteExistente.setDatanasc(dto.getDatanasc());

        if (dto.getSenha() != null && !dto.getSenha().trim().isEmpty()) {
            clienteExistente.setSenha(passwordEncoder.encode(dto.getSenha()));
        }

        atualizarEnderecosManualmente(clienteExistente, dto.getEnderecos());
        atualizarCartoesManualmente(clienteExistente, dto.getCartoes());

        Cliente salvo = clienteRepository.save(clienteExistente);
        return clienteMapper.toDTO(salvo);
    }

    private void atualizarEnderecosManualmente(Cliente cliente, List<EnderecoDTO> dtos) {
        Set<Endereco> novosEnderecos = new HashSet<>();
        if (dtos != null) {
            for (EnderecoDTO endDto : dtos) {
                Endereco endereco;
                if (endDto.getId() != null && endDto.getId() != 0) {
                    endereco = enderecoRepository.findById(endDto.getId()).orElse(null);
                    if (endereco != null)
                        enderecoMapper.updateEntityFromDto(endDto, endereco);
                } else {
                    endereco = enderecoService.saveOrReuseEndereco(endDto);
                    endereco.getClientes().add(cliente);
                }
                if (endereco != null)
                    novosEnderecos.add(endereco);
            }
        }
        cliente.setEnderecos(novosEnderecos);
    }

    private void atualizarCartoesManualmente(Cliente cliente, List<CartaoDTO> dtos) {
        Set<Cartao> novosCartoes = new HashSet<>();
        if (dtos != null) {
            for (CartaoDTO cartaoDto : dtos) {
                Cartao cartao;
                if (cartaoDto.getId() != null && cartaoDto.getId() != 0) {
                    cartao = cartaoRepository.findById(cartaoDto.getId()).orElse(null);
                } else {
                    cartao = cartaoService.saveOrReuseCartao(cartaoDto);
                    cartao.getClientes().add(cliente);
                }
                if (cartao != null)
                    novosCartoes.add(cartao);
            }
        }
        cliente.setCartoes(novosCartoes);
    }

    private void validarDadosSignup(SignupDTO dto) {
        if (clienteRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
        if (dto.getSenha().length() < 8) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
        }
    }

    public Optional<ClienteDTO> autenticarCliente(String email, String senha) {
        return clienteRepository.findByEmail(email)
                .filter(c -> passwordEncoder.matches(senha, c.getSenha()))
                .map(clienteMapper::toDTO);
    }
}