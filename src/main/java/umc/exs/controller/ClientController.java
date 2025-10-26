package umc.exs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.service.ClientService;

import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClientService clientService;

    // -------------------------------------------------------------------------
    // --- CRUD Cliente
    // -------------------------------------------------------------------------
 
    @PostMapping
    public ResponseEntity<Cliente> criarCliente(@RequestBody Cliente cliente) {
        Cliente novoCliente = clientService.salvarCliente(cliente);
        return new ResponseEntity<>(novoCliente, HttpStatus.CREATED); // Retorna 201 Created
    }


    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarCliente(@PathVariable Long id) {
        Optional<Cliente> cliente = clientService.buscarClientePorId(id);
        return cliente.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                      .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND)); // Retorna 404 Not Found
    }


    @PutMapping("/{id}")
    public ResponseEntity<Cliente> atualizarCliente(@PathVariable Long id, @RequestBody Cliente clienteDetails) {
        Optional<Cliente> clienteOpt = clientService.buscarClientePorId(id);
        
        if (clienteOpt.isPresent()) {
            Cliente cliente = clienteOpt.get();

            cliente.setNome(clienteDetails.getNome());
            cliente.setDatanasc(clienteDetails.getDatanasc());
            cliente.setGen(clienteDetails.getGen());
            cliente.setEmail(clienteDetails.getEmail());
            
            Cliente clienteAtualizado = clientService.salvarCliente(cliente);
            return new ResponseEntity<>(clienteAtualizado, HttpStatus.OK); // Retorna 200 OK
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarCliente(@PathVariable Long id) {
        Optional<Cliente> clienteOpt = clientService.buscarClientePorId(id);
        if (clienteOpt.isPresent()) {
            clientService.deletarCliente(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Retorna 204 No Content
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // -------------------------------------------------------------------------
    // --- Endereços (Relacionamentos)
    // -------------------------------------------------------------------------

    @PostMapping("/{clienteId}/endereco")
    public ResponseEntity<Endereco> adicionarEndereco(@PathVariable Long clienteId, @RequestBody Endereco endereco) {
        Endereco novoEndereco = clientService.salvarEndereco(clienteId, endereco);
        if (novoEndereco != null) {
            return new ResponseEntity<>(novoEndereco, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Cliente não encontrado
        }
    }

    @GetMapping("/{clienteId}/enderecos")
    public ResponseEntity<Set<Endereco>> buscarEnderecos(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clientService.buscarClientePorId(clienteId);
        
        if (clienteOpt.isPresent()) {

            return new ResponseEntity<>(clienteOpt.get().getEnderecos(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    
    // -------------------------------------------------------------------------
    // --- Cartões (Relacionamentos)
    // -------------------------------------------------------------------------

    @PostMapping("/{clienteId}/cartao")
    public ResponseEntity<Cartao> adicionarCartao(@PathVariable Long clienteId, @RequestBody Cartao cartao) {
        Cartao novoCartao = clientService.salvarCartao(clienteId, cartao);
        if (novoCartao != null) {
            return new ResponseEntity<>(novoCartao, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Cliente não encontrado
        }
    }

    @GetMapping("/{clienteId}/cartoes")
    public ResponseEntity<Set<Cartao>> buscarCartoes(@PathVariable Long clienteId) {
        Optional<Cliente> clienteOpt = clientService.buscarClientePorId(clienteId);
        
        if (clienteOpt.isPresent()) {
            // Retorna a coleção de cartões do cliente
            return new ResponseEntity<>(clienteOpt.get().getCartoes(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
