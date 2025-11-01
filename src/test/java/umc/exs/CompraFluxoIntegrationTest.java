package umc.exs;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import umc.exs.backstage.service.CarrinhoService;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.compras.ItemCarrinho;
import umc.exs.model.entidades.Cartao;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.ProdutoRepository;

@SpringBootTest
public class CompraFluxoIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CarrinhoService carrinhoService;

    @Test
    public void full_purchase_flow_using_repositories_and_service() throws Exception {
        // 1) criar cliente (persistir)
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Teste");
        cliente.setEmail("cliente+" + System.currentTimeMillis() + "@example.com");
        cliente.setSenha("senha-plain");
        
        cliente.setDatanasc(LocalDate.of(1990, 1, 1).toString());
        Cliente savedCliente = clienteRepository.save(cliente);
        assertThat(savedCliente.getId()).isNotNull();

        // 2) criar e salvar endereco antes de associar
        Endereco endereco = new Endereco();
        endereco.setRua("Rua Teste");
        endereco.setCidade("Cidade");
        endereco.setBairro("Centro");
        endereco.setNumero("100");
        endereco.setCep("01234-567");
        Endereco savedEndereco = enderecoRepository.save(endereco);
        assertThat(savedEndereco.getId()).isNotNull();

        // associar endereco ao cliente
        Set<Endereco> enderecos = savedCliente.getEnderecos();
        if (enderecos == null) enderecos = new HashSet<>();
        enderecos.add(savedEndereco);
        savedCliente.setEnderecos(enderecos);
        savedCliente = clienteRepository.save(savedCliente);

        // 3) criar e salvar cartao antes de associar
        Cartao cartao = new Cartao();
        cartao.setNumero("4111111111111111");
        cartao.setNomeTitular(savedCliente.getNome());
        cartao.setCvv("123");
        cartao.setValidade("12/30");
        Cartao savedCartao = cartaoRepository.save(cartao);
        assertThat(savedCartao.getId()).isNotNull();

        // associar cartao ao cliente
        Set<Cartao> cartoes = savedCliente.getCartoes();
        if (cartoes == null) cartoes = new HashSet<>();
        cartoes.add(savedCartao);
        savedCliente.setCartoes(cartoes);
        savedCliente = clienteRepository.save(savedCliente);

        // 4) criar e salvar produto
        Produto produto = new Produto();
        produto.setTitulo("Produto Teste");
        produto.setPrecificacao(120.0f);
        Produto savedProduto = produtoRepository.save(produto);
        assertThat(savedProduto.getId()).isNotNull();

        // 5) construir carrinho em memória
        Carrinho carrinho = new Carrinho();
        carrinho.setEndereco(savedEndereco); // associa endereço
        // itens
        ItemCarrinho item = new ItemCarrinho();
        item.setProduto(savedProduto); 
        item.setQuantidade(1);
        carrinho.getItens().add(item);

        // 6) finalizar compra pelo serviço usando Cliente
        boolean result = carrinhoService.finalizarCompra(savedCliente);

        // 7) validar que compra foi realizada
        assertThat(result).isTrue();
    }
}
