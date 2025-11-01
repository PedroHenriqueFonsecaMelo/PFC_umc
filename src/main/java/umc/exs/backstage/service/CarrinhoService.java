package umc.exs.backstage.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.DTO.CarrinhoDTO;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.ItemCarrinho;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.CupomRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.ProdutoRepository;

@Service
public class CarrinhoService {

    private Carrinho carrinho = new Carrinho();

    @Autowired
    private ProdutoRepository produtoRepository;
    @Autowired
    private CupomRepository cupomRepository;
    @Autowired
    private EnderecoRepository enderecoRepository;

    // Adiciona produto ao carrinho
    public CarrinhoDTO addProduto(Long produtoId, int quantidade) {
        Produto produto = buscarProduto(produtoId);
        if (produto == null)
            throw new RuntimeException("Produto não encontrado");
        carrinho.getItens().add(new ItemCarrinho(produto, quantidade));
        recalcularTotal();
        return CarrinhoDTO.fromEntity(carrinho);
    }

    // Remove produto do carrinho
    public CarrinhoDTO removeProduto(Long produtoId) {
        carrinho.getItens().removeIf(i -> i.getProduto().getId().equals(produtoId));
        recalcularTotal();
        return CarrinhoDTO.fromEntity(carrinho);
    }

    // Aplica um cupom ao carrinho
    public CarrinhoDTO aplicarCupom(String codigo) {
        Cupom cupom = buscarCupom(codigo);
        carrinho.setCupom(cupom);
        recalcularTotal();
        return CarrinhoDTO.fromEntity(carrinho);
    }

    // Define o endereço de entrega para o carrinho
    public CarrinhoDTO setEndereco(Long enderecoId) {
        Endereco endereco = buscarEndereco(enderecoId);
        carrinho.setEndereco(endereco);
        return CarrinhoDTO.fromEntity(carrinho);
    }

    // Define os pagamentos para o carrinho
    public CarrinhoDTO setPagamentos(List<PagamentoDTO> pagamentos) {
        if (pagamentos == null || pagamentos.isEmpty()) {
            throw new RuntimeException("É necessário informar pelo menos um pagamento.");
        }

        BigDecimal soma = BigDecimal.ZERO;
        for (PagamentoDTO p : pagamentos) {
            if (p.getCartaoId() == null) {
                throw new RuntimeException("Cartão não informado em um dos pagamentos.");
            }

            if (p.getValor().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Valor inválido para o cartão " + p.getCartaoId());
            }
            soma = soma.add(p.getValor()); // Usa BigDecimal para somar valores
        }

        BigDecimal totalCarrinho = new BigDecimal(carrinho.getTotal());
        if (soma.compareTo(totalCarrinho) != 0) { // Usa compareTo para comparar BigDecimal
            throw new RuntimeException("A soma dos pagamentos (" + soma +
                    ") é diferente do total do carrinho (" + totalCarrinho + ").");
        }

        carrinho.setPagamentos(pagamentos);
        return CarrinhoDTO.fromEntity(carrinho);
    }

    /**
     * Finaliza a compra para o cliente fornecido.
     * Garante que um endereço válido seja usado (tenta obter do cliente, senão do
     * repository).
     */
    @Transactional
    public boolean finalizarCompra(Cliente cliente) {
        if (cliente == null)
            throw new IllegalArgumentException("cliente is required");

        carrinho = this.getCarrinhoAtual();

        Endereco endereco = null;
        Set<Endereco> enderecosCliente = cliente.getEnderecos();
        if (enderecosCliente != null && !enderecosCliente.isEmpty()) {
            endereco = enderecosCliente.iterator().next();
        }

        if (endereco == null) {
            List<Endereco> possiveis = enderecoRepository.findAll();
            Optional<Endereco> found = possiveis.stream()
                    .filter(e -> {
                        try {
                            Set<Cliente> cs = e.getClientes();
                            return cs != null && cs.stream().anyMatch(c -> c.getId().equals(cliente.getId()));
                        } catch (Exception ex) {
                            return false;
                        }
                    })
                    .findFirst();
            if (found.isPresent())
                endereco = found.get();
        }

        if (carrinho.getEndereco() == null && endereco != null) {
            carrinho.setEndereco(endereco);
        }

        if (carrinho.getEndereco() == null) {
            throw new RuntimeException("Endereço não definido");
        }

        // Compra finalizada com sucesso
        return true;
    }

    // Retorna o carrinho atual
    public CarrinhoDTO getCarrinho() {
        return CarrinhoDTO.fromEntity(carrinho);
    }

    private void recalcularTotal() {

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal frete = new BigDecimal(Math.random() * 20 + 5);

        for (ItemCarrinho item : carrinho.getItens()) {
            BigDecimal precoUnitario = item.getProduto().getPrecificacao();
            BigDecimal quantidade = new BigDecimal(item.getQuantidade());
            total = total.add(precoUnitario.multiply(quantidade)); // Multiplica usando BigDecimal
        }

        if (carrinho.getCupom() != null) {
            total = total.subtract(new BigDecimal(carrinho.getCupom().getValor())); // Subtrai o cupom
        }

        // Somar o valor do frete
        total = total.add(frete);

        carrinho.setFrete(frete.floatValue()); // Convertendo o BigDecimal de volta para float para o carrinho
        carrinho.setTotal(total.floatValue()); // Faz o mesmo para o total final
    }

    private Produto buscarProduto(Long id) {
        return produtoRepository.findById(id).orElse(null);
    }

    private Cupom buscarCupom(String codigo) {
        return cupomRepository.findByCodigo(codigo).orElse(null);
    }

    private Endereco buscarEndereco(Long id) {
        return enderecoRepository.findById(id).orElse(null);
    }

    private Carrinho getCarrinhoAtual() {
        return this.carrinho;
    }
}
