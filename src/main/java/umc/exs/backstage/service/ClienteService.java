package umc.exs.backstage.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.DTO.user.ClienteDTO;
import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private EnderecoRepository enderecoRepository;
    
    @Autowired
    private CartaoRepository cartaoRepository;

    // -----------------------
    // CRUD de Cliente
    // -----------------------

    public ClienteDTO salvarCliente(ClienteDTO clienteDTO) {
        Cliente cliente = clienteDTO.toEntity();
        Cliente salvo = clienteRepository.save(cliente);
        return ClienteDTO.fromEntity(salvo);
    }

    public Optional<ClienteDTO> buscarClientePorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteDTO::fromEntity);
    }

    public List<ClienteDTO> listarTodos() {
        return clienteRepository.findAll()
                .stream()
                .map(ClienteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public void deletarCliente(Long id) {
        clienteRepository.deleteById(id);
    }

    // -----------------------
    // CRUD de Endereço
    // -----------------------

    public Endereco salvarEndereco(Long clienteId, Endereco endereco) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

        // Adiciona a relação
        endereco.getClientes().add(cliente);
        Endereco salvo = enderecoRepository.save(endereco);

        // Atualiza cliente apenas uma vez
        cliente.getEnderecos().add(salvo);
        clienteRepository.save(cliente);

        return salvo;
    }

    public List<Endereco> buscarEnderecosPorCliente(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));
        return List.copyOf(cliente.getEnderecos());
    }

    // -----------------------
    // CRUD de Cartão
    // -----------------------

    public Cartao salvarCartao(Long clienteId, Cartao cartao) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

        Cartao salvo = cartaoRepository.save(cartao);
        cliente.getCartoes().add(salvo);
        clienteRepository.save(cliente);

        return salvo;
    }

    public ClienteDTO salvarCliente(SignupDTO signupDTO) {
        Cliente c = new Cliente();
        c.setNome(signupDTO.getNome());
        c.setEmail(signupDTO.getEmail());
        c.setSenha(signupDTO.getSenha());
        c.setDatanasc(signupDTO.getDatanasc());
        c.setGen(signupDTO.getGen());
        Cliente salvo = clienteRepository.save(c);
        return ClienteDTO.fromEntity(salvo);
    }
}
