package umc.exs.backstage.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.DTO.PagamentoDTO;
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

    

    public Carrinho addProduto(Long produtoId, int quantidade) {

        Produto produto = buscarProduto(produtoId);
        if (produto == null)
            throw new RuntimeException("Produto não encontrado");
        carrinho.getItens().add(new ItemCarrinho(produto, quantidade));
        recalcularTotal();
        return carrinho;
    }

    public Carrinho removeProduto(Long produtoId) {
        carrinho.getItens().removeIf(i -> i.getProduto().getId().equals(produtoId));
        recalcularTotal();
        return carrinho;
    }

    public Carrinho aplicarCupom(String codigo) {
        // Buscar cupom no banco
        Cupom cupom = buscarCupom(codigo);
        carrinho.setCupom(cupom);
        recalcularTotal();
        return carrinho;
    }

    public Carrinho setEndereco(Long enderecoId) {
        // Buscar endereço no banco
        Endereco endereco = buscarEndereco(enderecoId);
        carrinho.setEndereco(endereco);
        return carrinho;
    }

    public Carrinho setPagamentos(List<PagamentoDTO> pagamentos) {
        if (pagamentos == null || pagamentos.isEmpty()) {
            throw new RuntimeException("É necessário informar pelo menos um pagamento.");
        }

        float soma = 0f;
        for (PagamentoDTO p : pagamentos) {
            if (p.getCartaoId() == null) {
                throw new RuntimeException("Cartão não informado em um dos pagamentos.");
            }

            if (p.getValor() <= 0f) {
                throw new RuntimeException("Valor inválido para o cartão " + p.getCartaoId());
            }
            soma += p.getValor();
        }

        float totalCarrinho = carrinho.getTotal();
        if (Math.abs(soma - totalCarrinho) > 0.01f) {
            throw new RuntimeException("A soma dos pagamentos (" + soma +
                    ") é diferente do total do carrinho (" + totalCarrinho + ").");
        }

        carrinho.setPagamentos(pagamentos);
        return carrinho;
    }

    /**
     * Finaliza a compra para o cliente fornecido.
     * Garante que um endereço válido seja usado (tenta obter do cliente, senão do repository).
     */
    @Transactional
    public boolean finalizarCompra(Cliente cliente) {
        if (cliente == null) throw new IllegalArgumentException("cliente is required");

        Carrinho carrinho = this.getCarrinhoAtual(); 

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
            if (found.isPresent()) endereco = found.get();
        }

        if (carrinho.getEndereco() == null && endereco != null) {
            carrinho.setEndereco(endereco);
        }

        // se ainda não houver endereço, lançar exceção com mensagem clara (preserva semântica)
        if (carrinho.getEndereco() == null) {
            throw new RuntimeException("Endereço não definido");
        }
        // ---------------------------------------------------------------

        return true;
    }

    public Carrinho getCarrinho() {
        return carrinho;
    }

    private void recalcularTotal() {
        float total = 0;
        for (ItemCarrinho item : carrinho.getItens()) {
            total += item.getProduto().getPrecificacao() * item.getQuantidade();
        }
        float frete = (float) (Math.random() * 20 + 5);
        if (carrinho.getCupom() != null)
            total -= carrinho.getCupom().getValor();
        carrinho.setFrete(frete);
        carrinho.setTotal(total + frete);
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
