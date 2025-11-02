package umc.exs;

import java.time.LocalDate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import umc.exs.model.DTO.purchase.PagamentoDTO;
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
import umc.exs.backstage.service.CarrinhoService;

@SpringBootTest
public class CompraMultiplosCartoesTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CartaoRepository cartaoRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CarrinhoService carrinhoService;

    @Test
    public void testCompraComMultiplosCartoesEProdutos() throws Exception {

        // --- Criar cliente ---
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Teste Cartoes");
        cliente.setEmail("cliente+" + System.currentTimeMillis() + "@example.com");
        cliente.setSenha("senha-plain");
        cliente.setGen("M");
        cliente.setDatanasc(LocalDate.of(1990, 1, 1).toString());
        cliente = clienteRepository.save(cliente);

        // --- Criar endereço ---
        Endereco endereco = new Endereco();
        endereco.setRua("Rua A");
        endereco.setCidade("Cidade B");
        endereco.setBairro("Bairro C");
        endereco.setNumero("123");
        endereco.setCep("00000-000");
        endereco = enderecoRepository.save(endereco);
        cliente.getEnderecos().add(endereco);
        cliente = clienteRepository.save(cliente);

        // --- Criar cartões ---
        Cartao cartao1 = new Cartao();
        cartao1.setNumero("4111111111111111");
        cartao1.setNomeTitular(cliente.getNome());
        cartao1.setCvv("123");
        cartao1.setValidade("12/30");
        cartao1 = cartaoRepository.save(cartao1);

        Cartao cartao2 = new Cartao();
        cartao2.setNumero("5500000000000004");
        cartao2.setNomeTitular(cliente.getNome());
        cartao2.setCvv("456");
        cartao2.setValidade("11/29");
        cartao2 = cartaoRepository.save(cartao2);

        cliente.getCartoes().add(cartao1);
        cliente.getCartoes().add(cartao2);
        cliente = clienteRepository.save(cliente);

        // --- Criar produtos ---
        Produto produto1 = new Produto();
        produto1.setTitulo("Produto 1");
        produto1.setPrecificacao(100.0f);
        produto1 = produtoRepository.save(produto1);

        Produto produto2 = new Produto();
        produto2.setTitulo("Produto 2");
        produto2.setPrecificacao(200.0f);
        produto2 = produtoRepository.save(produto2);

        // --- Criar carrinho ---
        Carrinho carrinho = new Carrinho();
        carrinho.setEndereco(endereco);

        ItemCarrinho item1 = new ItemCarrinho();
        item1.setProduto(produto1);
        item1.setQuantidade(2);

        ItemCarrinho item2 = new ItemCarrinho();
        item2.setProduto(produto2);
        item2.setQuantidade(1);

        carrinho.setItens(Arrays.asList(item1, item2));

        // --- Criar pagamentos ---
        PagamentoDTO pagamento1 = new PagamentoDTO();
        pagamento1.setCartaoId(cartao1.getId());
        pagamento1.setValor(150.0f); // paga parte do total

        PagamentoDTO pagamento2 = new PagamentoDTO();
        pagamento2.setCartaoId(cartao2.getId());
        pagamento2.setValor(250.0f); // paga o restante

        carrinho.setPagamentos(Arrays.asList(pagamento1, pagamento2));

        // --- Calcular total ---
        float totalCarrinho = item1.calcularSubtotal().add(item2.calcularSubtotal()).floatValue();
        carrinho.setTotal(totalCarrinho);

        // --- Finalizar compra ---
        boolean sucesso = carrinhoService.finalizarCompra(cliente);

        // --- Validar ---
        assertThat(sucesso).isTrue();
        assertThat(carrinho.getTotal()).isEqualTo(400.0f);
        assertThat(carrinho.getPagamentos().stream().mapToDouble(p -> p.getValor().doubleValue()).sum()).isEqualTo(400.0);
    }
}
