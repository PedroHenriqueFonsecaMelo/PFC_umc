package umc.exs.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.ItemCarrinho;
import umc.exs.model.compras.Pedido;
import umc.exs.model.compras.PedidoItem;
import umc.exs.model.entidades.Cliente;
import umc.exs.model.entidades.Endereco;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.CupomRepository;
import umc.exs.repository.EnderecoRepository;
import umc.exs.repository.PedidoRepository;
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
    @Autowired
    private PedidoRepository pedidoRepository;

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

    public boolean finalizarCompra(Cliente cliente) {
        if (carrinho.getItens().isEmpty()) {
            throw new RuntimeException("Carrinho vazio");
        }
        if (carrinho.getPagamentos() == null || carrinho.getPagamentos().isEmpty()) {
            throw new RuntimeException("Nenhum pagamento definido");
        }
        if (carrinho.getEndereco() == null) {
            throw new RuntimeException("Endereço não definido");
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setEnderecoId(carrinho.getEndereco().getId());
        pedido.setTotal((double) carrinho.getTotal());
        pedido.setStatus("PENDENTE");

        // Criar itens do pedido
        List<PedidoItem> itensPedido = new ArrayList<>();
        for (ItemCarrinho itemCarrinho : carrinho.getItens()) {
            PedidoItem itemPedido = new PedidoItem();
            itemPedido.setProdutoId(itemCarrinho.getProduto().getId());
            itemPedido.setQuantidade(itemCarrinho.getQuantidade());
            itemPedido.setPrecoUnitario((double) itemCarrinho.getProduto().getPrecificacao());
            itemPedido.setPedido(pedido);
            itensPedido.add(itemPedido);
        }
        pedido.setItens(itensPedido);

        pedidoRepository.save(pedido);

        carrinho = new Carrinho();

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
}
