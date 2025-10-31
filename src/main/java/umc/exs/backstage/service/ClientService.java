package umc.exs.backstage.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.ClienteRepository;

@Service
public class ClientService {

    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private EnderecoRepository enderecoRepository;
    @Autowired
    private CartaoRepository cartaoRepository;

    // --- CRUD Cliente ---

    public Cliente salvarCliente(Cliente cliente) {

        return clienteRepository.save(cliente);
    }

    public Optional<Cliente> buscarClientePorId(Long id) {
        return clienteRepository.findById(id);
    }

    // --- CRUD Endereço ---

    public Endereco salvarEndereco(Long clienteId, Endereco endereco) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            // Adiciona o cliente ao endereço antes de salvar
            endereco.getClientes().add(cliente);

            // Salva o endereço já com a relação
            Endereco savedEndereco = enderecoRepository.save(endereco);

            // Atualiza o cliente com o endereço salvo
            cliente.getEnderecos().add(savedEndereco);

            // Salva o cliente apenas uma vez
            clienteRepository.save(cliente);

            return savedEndereco;
        }
        return null;
    }

    public List<Endereco> buscarEnderecosPorCliente(Long clienteId) {

        return clienteRepository.findById(clienteId)
                .map(cliente -> List.copyOf(cliente.getEnderecos()))
                .orElse(List.of());
    }

    // --- CRUD Cartão ---

    public Cartao salvarCartao(Long clienteId, Cartao cartao) {
        Optional<Cliente> clienteOpt = clienteRepository.findById(clienteId);
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();
            Cartao savedCartao = cartaoRepository.save(cartao);
            cliente.getCartoes().add(savedCartao);
            clienteRepository.save(cliente);
            return savedCartao;
        }
        return null;
    }

    public void deletarCliente(Long id) {

        clienteRepository.deleteById(id);
    }
}