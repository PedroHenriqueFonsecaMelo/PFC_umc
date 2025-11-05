package umc.exs.backstage.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;
// Importação dos Mappers (Assumindo que estão em umc.exs.model.daos.mappers)
import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.mappers.ClienteMapper;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.model.entidades.usuario.Cartao;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final EnderecoRepository enderecoRepository;
    private final CartaoRepository cartaoRepository;
    private PasswordEncoder passwordEncoder;

    // Construtor para Injeção de Dependência
    public ClienteService(ClienteRepository clienteRepository,
            EnderecoRepository enderecoRepository,
            CartaoRepository cartaoRepository) {
        this.clienteRepository = clienteRepository;
        this.enderecoRepository = enderecoRepository;
        this.cartaoRepository = cartaoRepository;
    }

    // ===========================
    // 🔹 SALVAR CLIENTE SIMPLES (Existente)
    // ===========================
    @Transactional // Adicionado Transactional, se for uma operação de persistência
    public ClienteDTO salvarCliente(ClienteConvertible dto) {
        Cliente cliente = ClienteMapper.toEntity(dto);
        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    // ===========================
    // 🔹 SALVAR CLIENTE COMPLETO (Novo Cadastro)
    // ===========================
    @Transactional
    public ClienteDTO salvarClienteCompleto(SignupDTO signupDTO, EnderecoDTO enderecoDTO, CartaoDTO cartaoDTO) {
        Cliente cliente = ClienteMapper.toEntity(signupDTO);

        if (enderecoDTO != null) {
            cliente.getEnderecos().add(EnderecoMapper.toEntity(enderecoDTO));
        }
        if (cartaoDTO != null) {
            cliente.getCartoes().add(CartaoMapper.toEntity(cartaoDTO));
        }

        Cliente salvo = clienteRepository.save(cliente);
        return ClienteMapper.fromEntity(salvo);
    }

    // ===========================
    // 🔹 BUSCAS
    // ===========================
    public Optional<ClienteDTO> buscarClientePorEmail(String email) {
        return clienteRepository.findByEmail(email)
                .map(ClienteMapper::fromEntity);
    }

    public Optional<ClienteDTO> buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteMapper::fromEntity);
    }

    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll().stream()
                .map(ClienteMapper::fromEntity)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // 📢 ATUALIZAÇÃO UNIFICADA (Simples + Coleções) - NOVO MÉTODO
    // ==========================================================
    @Transactional
    public ClienteDTO atualizarClienteEAssociacoes(Long clienteId, ClienteDTO clienteAtualizadoDTO) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // 1. ATUALIZAÇÃO DOS CAMPOS SIMPLES
        // Copia campos simples (nome, cpf, etc.) do DTO para a Entity, ignorando ID,
        // SENHA e as coleções.
        if (clienteAtualizadoDTO.getSenha() != null && !clienteAtualizadoDTO.getSenha().isBlank()) {

            clienteAtualizadoDTO.setSenha(passwordEncoder.encode(clienteAtualizadoDTO.getSenha()));
            BeanUtils.copyProperties(clienteAtualizadoDTO, cliente, "id", "enderecos", "cartoes");
        } else {
            
            BeanUtils.copyProperties(clienteAtualizadoDTO, cliente, "id", "senha", "enderecos", "cartoes");
        }

        // 2. CONVERSÃO E ATUALIZAÇÃO DAS COLEÇÕES
        // Converte a List<DTO> (amigável ao formulário) de volta para Set<DTO>
        // (necessário para a lógica de merge).
        Set<EnderecoDTO> novosEnderecosSet = clienteAtualizadoDTO.getEnderecos() != null
                ? clienteAtualizadoDTO.getEnderecos().stream().collect(Collectors.toSet())
                : Collections.emptySet(); // Garante Set vazio se for null

        Set<CartaoDTO> novosCartoesSet = clienteAtualizadoDTO.getCartoes() != null
                ? clienteAtualizadoDTO.getCartoes().stream().collect(Collectors.toSet())
                : Collections.emptySet(); // Garante Set vazio se for null

        // Chamamos o método existente (otimizado) que lida com o merge, update e delete
        // das coleções.
        atualizarEnderecosECartoes(
                clienteId,
                novosEnderecosSet,
                novosCartoesSet);

        // O método acima já salvou a Entity 'cliente', mas retornamos a versão mais
        // atualizada.
        return ClienteMapper.fromEntity(cliente);
    }

    // ====================================================================
    // 🔹 ATUALIZAR ENDEREÇOS E/OU CARTÕES (Lógica Otimizada O(N) Reutilizada)
    // ====================================================================
    @Transactional
    public Cliente atualizarEnderecosECartoes(Long clienteId,
            Set<EnderecoDTO> novosEnderecosDTO, // Recebe SET
            Set<CartaoDTO> novosCartoesDTO) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        // --- ATUALIZA ENDEREÇOS ---
        if (novosEnderecosDTO != null) {
            Set<Endereco> enderecosAtuais = cliente.getEnderecos();

            // 1. Definição dos IDs que devem existir
            Set<Long> idsNovos = novosEnderecosDTO.stream()
                    .map(EnderecoDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 2. Remoção (Mantém apenas os endereços cujos IDs estão no Set de IDs Novos)
            enderecosAtuais.removeIf(e -> e.getId() != null && !idsNovos.contains(e.getId()));

            // 3. OTIMIZAÇÃO: Cria um Map para busca rápida O(1)
            Map<Long, Endereco> mapEnderecosAtuais = enderecosAtuais.stream()
                    .filter(e -> e.getId() != null)
                    .collect(Collectors.toMap(Endereco::getId, Function.identity()));

            // 4. Atualiza ou Adiciona
            for (EnderecoDTO dto : novosEnderecosDTO) {
                Endereco novaEntidade = EnderecoMapper.toEntity(dto);

                if (dto.getId() != null) {
                    Endereco existente = mapEnderecosAtuais.get(dto.getId());

                    if (existente != null) {
                        // 4a. Atualiza (copia as propriedades do novo DTO para a entidade existente)
                        BeanUtils.copyProperties(novaEntidade, existente, "id");
                    } else {
                        // 4b. Adiciona (Se o ID veio, mas não estava na coleção original, adiciona)
                        enderecosAtuais.add(novaEntidade);
                    }
                } else {
                    // 4c. Adiciona novo (sem ID)
                    enderecosAtuais.add(novaEntidade);
                }
            }
        }

        // --- ATUALIZA CARTÕES --- (Lógica de cartão omitida por brevidade, mas deve
        // ser idêntica)
        if (novosCartoesDTO != null) {
            Set<Cartao> cartoesAtuais = cliente.getCartoes();
            // ... Lógica similar de merge, removeIf, Map e BeanUtils.copyProperties para
            // Cartões
            Set<Long> idsNovos = novosCartoesDTO.stream()
                    .map(CartaoDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            cartoesAtuais.removeIf(c -> c.getId() != null && !idsNovos.contains(c.getId()));

            Map<Long, Cartao> mapCartoesAtuais = cartoesAtuais.stream()
                    .filter(c -> c.getId() != null)
                    .collect(Collectors.toMap(Cartao::getId, Function.identity()));

            for (CartaoDTO dto : novosCartoesDTO) {
                Cartao novaEntidade = CartaoMapper.toEntity(dto);

                if (dto.getId() != null) {
                    Cartao existente = mapCartoesAtuais.get(dto.getId());

                    if (existente != null) {
                        BeanUtils.copyProperties(novaEntidade, existente, "id");
                    } else {
                        cartoesAtuais.add(novaEntidade);
                    }
                } else {
                    cartoesAtuais.add(novaEntidade);
                }
            }
        }

        // 5. Salva o cliente (o Hibernate persiste as mudanças nas coleções)
        return clienteRepository.save(cliente);
    }

    // ===========================
    // 🔹 DELEÇÃO INDIVIDUAL
    // ===========================
    @Transactional
    public void deletarEnderecoDoCliente(Long clienteId, Long enderecoId) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.getEnderecos().removeIf(endereco -> endereco.getId().equals(enderecoId));
            clienteRepository.save(cliente);
            enderecoRepository.deleteById(enderecoId);
        });
    }

    @Transactional
    public void deletarCartaoDoCliente(Long clienteId, Long cartaoId) {
        clienteRepository.findById(clienteId).ifPresent(cliente -> {
            cliente.getCartoes().removeIf(cartao -> cartao.getId().equals(cartaoId));
            clienteRepository.save(cliente);
            cartaoRepository.deleteById(cartaoId);
        });
    }

}