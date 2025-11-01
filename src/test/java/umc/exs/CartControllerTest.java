package umc.exs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.ProdutoRepository;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Produto produto;
    private Cliente cliente;
    private Endereco endereco;

    @BeforeEach
    public void setup() {
        clienteRepository.deleteAll();
        produtoRepository.deleteAll();
        enderecoRepository.deleteAll();
        // Criar produto
        produto = new Produto();
        produto.setTitulo("Produto Teste");
        produto.setDescricaoDoProduto("Descrição do Produto Teste");
        produto.setPrecificacao(100f);
        produtoRepository.save(produto);

        // Criar cliente
        cliente = new Cliente();
        cliente.setNome("Cliente Teste");
        cliente.setSenha("senha123");
        cliente.setDatanasc("1990-01-01");
        cliente.setGen("M");
        cliente.setEmail("teste+" + System.currentTimeMillis() + "@email.com");
        clienteRepository.save(cliente);

        // Criar endereço
        endereco = new Endereco();
        endereco.setRua("Rua Teste");
        endereco.setCidade("Cidade Teste");
        enderecoRepository.save(endereco);
    }

    @Test
    public void testeFluxoCompletoCarrinho() throws Exception {

        // 1. Adicionar produto
        mockMvc.perform(post("/cart/add")
                .with(SecurityMockMvcRequestPostProcessors.user("teste").password("senha123").roles("USER"))
                .param("produtoId", produto.getId().toString())
                .param("quantidade", "1"))
                .andExpect(status().isOk());

        // 2. Definir endereço
        mockMvc.perform(post("/cart/endereco")
                .with(SecurityMockMvcRequestPostProcessors.user("teste").password("senha123").roles("USER"))
                .param("enderecoId", endereco.getId().toString()))
                .andExpect(status().isOk());

        // 3. Consultar carrinho para pegar total real (inclui frete)
        String carrinhoJson = mockMvc.perform(get("/cart")
                .with(SecurityMockMvcRequestPostProcessors.user("teste").password("senha123").roles("USER")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Carrinho carrinhoAtual = objectMapper.readValue(carrinhoJson, Carrinho.class);
        float total = carrinhoAtual.getTotal(); // valor real incluindo frete

        // 4. Criar pagamento com valor exato do carrinho
        PagamentoDTO pagamento = new PagamentoDTO();
        pagamento.setCartaoId(1L);
        pagamento.setValor(total);

        mockMvc.perform(post("/cart/pagamento")
                .with(SecurityMockMvcRequestPostProcessors.user("teste").password("senha123").roles("USER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(pagamento))))
                .andExpect(status().isOk());

        // 5. Finalizar compra
        mockMvc.perform(post("/cart/finalizar")
                .with(SecurityMockMvcRequestPostProcessors.user("teste").password("senha123").roles("USER"))
                .param("clienteId", cliente.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("Compra finalizada com sucesso!"));
    }
}
