package umc.exs.model.compras;

import java.util.ArrayList;
import java.util.List;

import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.Endereco;

public class Carrinho {
    private List<ItemCarrinho> itens = new ArrayList<>();
    private List<PagamentoDTO> pagamentos = new ArrayList<>();
    private Cupom cupom;
    private Endereco endereco;
    private float frete;
    private float total;

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public void setItens(List<ItemCarrinho> itens) {
        this.itens = itens;
    }

    public List<PagamentoDTO> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<PagamentoDTO> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public Cupom getCupom() {
        return cupom;
    }

    public void setCupom(Cupom cupom) {
        this.cupom = cupom;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public float getFrete() {
        return frete;
    }

    public void setFrete(float frete) {
        this.frete = frete;
    }

    public float getTotal() {
        return total;
    }

    public void setTotal(float total) {
        this.total = total;
    }
}
