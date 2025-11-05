package umc.exs.Teste;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@RestController
@RequestMapping("/test/cliente")
public class ControllerClienteTeste {
    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @PostMapping("/cadastrar-usuario")
    public ResponseEntity<Cliente> cadastrarUsuario() {
        // 1. Criar e salvar o endereço
        Endereco endereco = new Endereco();
        endereco.setPais("Brasil");
        endereco.setCep("01234-567");
        endereco.setEstado("SP");
        endereco.setCidade("São Paulo");
        endereco.setRua("Rua Teste");
        endereco.setBairro("Centro");
        endereco.setNumero("123");
        endereco.setComplemento("Apto 1");
        endereco.setTipoResidencia("Apartamento");
        endereco = enderecoRepository.save(endereco);

        // 2. Criar e salvar o cartão
        Cartao cartao = new Cartao();
        cartao.setNumero("1234567890123456");
        cartao.setBandeira("Visa");
        cartao.setNomeTitular("João da Silva");
        cartao.setValidade("12/30");
        cartao.setCvv("123");
        cartao.setPreferencial(true);
        cartao = cartaoRepository.save(cartao);

        // 3. Criar o cliente e associar cartão e endereço
        Cliente cliente = new Cliente();
        cliente.setNome("João da Silva");
        cliente.setEmail("joao@example.com");
        cliente.setSenha("senha123");
        cliente.setDatanasc("1990-01-01");
        cliente.setGen("M");

        cliente.getEnderecos().add(endereco);
        cliente.getCartoes().add(cartao);

        // Persistir cliente
        cliente = clienteRepository.save(cliente);

        return ResponseEntity.ok(cliente);
    }
}
