package umc.exs;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.controller.AdminController;
import umc.exs.model.Cartao;
import umc.exs.model.Cliente;
import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.Endereco;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.Pedido;
import umc.exs.model.compras.Troca;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.CartaoRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.CupomRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.PedidoRepository;
import umc.exs.repository.ProdutoRepository;
import umc.exs.repository.TrocaRepository;
import umc.exs.service.CarrinhoService;

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

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private TrocaRepository trocaRepository;

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private AdminController adminController;

    /**
     * Testa:
     * 1) Compra com 1 cartão
     * 2) Compra com múltiplos cartões
     * 3) Solicita troca para a primeira compra -> gera Troca
     * 4) Admin aprova troca -> gera Cupom
     * 5) Nova compra utilizando Cupom
     *
     * Observações: adapte getters/setters e chamadas de serviço caso suas
     * implementações tenham pequenas diferenças de assinatura.
     */

    @Test
    @Transactional
    public void testFluxoDeComprasComTrocaECupom() {
        // --- 1) Criar cliente, endereço, cartões e produto ---
        Cliente cliente = new Cliente();
        cliente.setNome("Teste Usuario");
        cliente.setEmail("teste@exemplo.com");
        cliente.setSenha("senha");
        cliente.setDatanasc("1990-01-01");
        cliente.setGen("M");
        cliente = clienteRepository.save(cliente);

        Endereco endereco = new Endereco();
        endereco.setPais("Brasil");
        endereco.setCep("01234-567");
        endereco.setEstado("SP");
        endereco.setCidade("São Paulo");
        endereco.setRua("Rua Teste");
        endereco.setBairro("Centro");
        endereco.setNumero("100");
        endereco.setComplemento("Apto 1");
        endereco.setTipoResidencia("Apartamento");
        endereco = enderecoRepository.save(endereco);

        Cartao cartao1 = new Cartao();
        cartao1.setNumero("1111222233334444");
        cartao1.setBandeira("VISA");
        cartao1.setNomeTitular("Teste Usuario");
        cartao1.setValidade("12/30");
        cartao1.setCvv("123");
        cartao1.setPreferencial(true);
        cartao1 = cartaoRepository.save(cartao1);

        Cartao cartao2 = new Cartao();
        cartao2.setNumero("5555666677778888");
        cartao2.setBandeira("Mastercard");
        cartao2.setNomeTitular("Teste Usuario");
        cartao2.setValidade("10/29");
        cartao2.setCvv("321");
        cartao2.setPreferencial(false);
        cartao2 = cartaoRepository.save(cartao2);

        cliente.getCartoes().add(cartao1);
        cliente.getCartoes().add(cartao2);
        cliente.getEnderecos().add(endereco);
        cliente = clienteRepository.save(cliente);

        Produto produto = new Produto();
        produto.setTitulo("Produto Teste");
        produto.setPrecificacao(100.0f);
        produto = produtoRepository.save(produto);

        // --- 2) Compra com 1 cartão ---
        Carrinho carrinho1 = carrinhoService.addProduto(produto.getId(), 1);
        System.out.println("Total após addProduto: " + carrinho1.getTotal());
        carrinho1 = carrinhoService.setEndereco(endereco.getId());
        System.out.println("Total após setEndereco: " + carrinho1.getTotal());

        PagamentoDTO pagamento1 = new PagamentoDTO();
        pagamento1.setCartaoId(cartao1.getId());
        pagamento1.setValor(carrinho1.getTotal()); // <-- aqui, usar o total real

        System.out.println("Somando pagamentos: " + pagamento1.getValor());
        carrinho1 = carrinhoService.setPagamentos(List.of(pagamento1));

        boolean finalizada1 = carrinhoService.finalizarCompra(cliente);
        assertTrue(finalizada1, "Compra 1 deve ser finalizada com sucesso");

        // Buscar o pedido da compra 1 — supondo que o último pedido corresponde
        List<Pedido> pedidos = pedidoRepository.findAll();
        assertFalse(pedidos.isEmpty(), "Deve existir ao menos um pedido no sistema");
        Pedido pedido1 = pedidos.get(pedidos.size() - 1);
        assertNotNull(pedido1.getId(), "Pedido 1 deve ter ID");

        // --- 3) Compra com múltiplos cartões ---
        Carrinho carrinho2 = carrinhoService.addProduto(produto.getId(), 2);
        carrinho2 = carrinhoService.setEndereco(endereco.getId());

        float total = carrinho2.getTotal();
        System.out.println("Total do carrinho antes do pagamento: " + total);

        PagamentoDTO pagamento2_1 = new PagamentoDTO();
        pagamento2_1.setCartaoId(cartao1.getId());
        pagamento2_1.setValor(total * 0.6f);

        PagamentoDTO pagamento2_2 = new PagamentoDTO();
        pagamento2_2.setCartaoId(cartao2.getId());
        pagamento2_2.setValor(total * 0.4f);

        carrinho2 = carrinhoService.setPagamentos(List.of(pagamento2_1, pagamento2_2));

        boolean finalizada2 = carrinhoService.finalizarCompra(cliente);
        assertTrue(finalizada2, "Compra 2 (multiplos cartões) deve finalizar com sucesso");

        // Buscar o pedido da compra 2
        List<Pedido> pedidosAtualizados = pedidoRepository.findAll();
        assertTrue(pedidosAtualizados.size() >= 2, "Devem existir ao menos dois pedidos");
        Pedido pedido2 = pedidosAtualizados.get(pedidosAtualizados.size() - 1);
        assertNotNull(pedido2.getId(), "Pedido 2 deve ter ID");

        // --- 4) Solicitar troca para a compra 1 ---
        Troca troca = new Troca();
        troca.setPedidoId(pedido1.getId());
        troca.setClienteId(cliente.getId());
        troca.setStatus("REQUESTED");
        troca.setValor(0.0);
        troca = trocaRepository.save(troca);
        assertNotNull(troca.getId(), "Troca deve ter sido salva e possuir ID");

        // --- 5) Admin aprova a troca e gera cupom ---
        ResponseEntity<?> resposta = adminController.approveExchange(troca.getId());
        assertEquals(HttpStatus.OK, resposta.getStatusCode(), "Admin.approveExchange deve retornar 200");

        @SuppressWarnings("unchecked")
        Map<String, Object> corpo = (Map<String, Object>) resposta.getBody();
        assertNotNull(corpo, "Resposta do admin não deve ser vazia");
        String codigoCupom = (String) corpo.get("cupom");
        assertNotNull(codigoCupom, "Deve existir código do cupom gerado");

        Cupom cupom = cupomRepository.findAll().stream()
                .filter(c -> codigoCupom.equals(c.getCodigo()))
                .findFirst()
                .orElse(null);
        assertNotNull(cupom, "Cupom gerado deve existir no banco");
        assertEquals(troca.getClienteId(), cupom.getClienteId(), "Cupom deve pertencer ao cliente");

        // --- 6) Nova compra usando o cupom ---
        Carrinho carrinho3 = carrinhoService.addProduto(produto.getId(), 1);
        carrinho3 = carrinhoService.setEndereco(endereco.getId());

        carrinho3 = carrinhoService.aplicarCupom(codigoCupom);

        PagamentoDTO pagamentoFinal = new PagamentoDTO();
        pagamentoFinal.setCartaoId(cartao1.getId());
        pagamentoFinal.setValor(carrinho3.getTotal()); // <-- usar o total real aqui também

        carrinho3 = carrinhoService.setPagamentos(List.of(pagamentoFinal));

        boolean finalizada3 = carrinhoService.finalizarCompra(cliente);
        assertTrue(finalizada3, "Compra com cupom deve finalizar com sucesso");

        // Verificar se o cupom foi marcado como usado
        Cupom cupomAtualizado = cupomRepository.findById(cupom.getId()).orElse(null);
        if (cupomAtualizado != null) {
            boolean usado = cupomAtualizado.isUsado();
            System.out.println("Cupom usado? " + usado);
        }

        System.out.println("Fluxo de compras com troca e cupom testado com sucesso.");
        System.out.println("IDs gerados:");
        System.out.println("Cliente ID: " + cliente.getId());
        System.out.println("Endereço ID: " + endereco.getId());
        System.out.println("Cartão 1 ID: " + cartao1.getId());
        System.out.println("Cartão 2 ID: " + cartao2.getId());
        System.out.println("Produto ID: " + produto.getId());
        System.out.println("Pedido 1 ID: " + pedido1.getId());
        System.out.println("Pedido 2 ID: " + pedido2.getId());
        System.out.println("Troca ID: " + troca.getId());
        System.out.println("Cupom Código: " + codigoCupom);

        System.out.println(carrinho1.toString());
        System.out.println(carrinho2.toString());
        System.out.println(carrinho3.toString());

    }
}
