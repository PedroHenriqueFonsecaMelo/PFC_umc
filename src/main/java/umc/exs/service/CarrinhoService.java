package umc.exs.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.model.Endereco;
import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.ItemCarrinho;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.*;

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
        // Buscar produto no banco (exemplo genérico)
        Produto produto = buscarProduto(produtoId);
        if (produto == null) throw new RuntimeException("Produto não encontrado");
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
        float soma = pagamentos.stream().map(PagamentoDTO::getValor).reduce(0f, Float::sum);
        if (Math.abs(soma - carrinho.getTotal()) > 0.01)
            throw new RuntimeException("Soma dos pagamentos diferente do total do carrinho");
        carrinho.setPagamentos(pagamentos);
        return carrinho;
    }

    public boolean finalizarCompra() {
        // Persistir compra, itens, pagamentos, etc.
        // Limpar carrinho
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
        float frete = (float) (Math.random() * 20 + 5); // frete aleatório
        if (carrinho.getCupom() != null) total -= carrinho.getCupom().getValor();
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

