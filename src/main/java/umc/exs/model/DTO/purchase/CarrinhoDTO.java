package umc.exs.model.DTO.purchase;

import java.util.List;
import java.util.stream.Collectors;

import umc.exs.model.DTO.admin.CupomDTO;
import umc.exs.model.DTO.user.EnderecoDTO;
import umc.exs.model.compras.Carrinho;


public class CarrinhoDTO {
    private List<ItemCarrinhoDTO> itens;
    private List<PagamentoDTO> pagamentos;
    private CupomDTO cupom;
    private EnderecoDTO endereco;
    private float frete;
    private float total;

    // Método estático para converter a entidade Carrinho em DTO
    public static CarrinhoDTO fromEntity(Carrinho carrinho) {
        if (carrinho == null) {
            return null;
        }
        CarrinhoDTO dto = new CarrinhoDTO();
        dto.setItens(carrinho.getItens().stream()
            .map(ItemCarrinhoDTO::fromEntity)
            .collect(Collectors.toList()));
        dto.setPagamentos(carrinho.getPagamentos());
        dto.setCupom(carrinho.getCupom() != null ? CupomDTO.fromEntity(carrinho.getCupom()) : null);
        dto.setEndereco(carrinho.getEndereco() != null ? EnderecoDTO.fromEntity(carrinho.getEndereco()) : null);
        dto.setFrete(carrinho.getFrete());
        dto.setTotal(carrinho.getTotal());
        return dto;
    }

    public Carrinho toEntity() {
        Carrinho carrinho = new Carrinho();
        carrinho.setItens(this.itens.stream()
            .map(ItemCarrinhoDTO::toEntity) 
            .collect(Collectors.toList()));
        carrinho.setPagamentos(this.pagamentos);
        carrinho.setCupom(this.cupom != null ? this.cupom.toEntity() : null);
        carrinho.setEndereco(this.endereco != null ? this.endereco.toEntity() : null);
        carrinho.setFrete(this.frete);
        carrinho.setTotal(this.total);
        return carrinho;
    }

    // Getters e setters
    public List<ItemCarrinhoDTO> getItens() {
        return itens;
    }
    public void setItens(List<ItemCarrinhoDTO> itens) {
        this.itens = itens;
    }

    public List<PagamentoDTO> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<PagamentoDTO> pagamentos) {
        this.pagamentos = pagamentos;
    }

    public CupomDTO getCupom() {
        return cupom;
    }

    public void setCupom(CupomDTO cupom) {
        this.cupom = cupom;
    }

    public EnderecoDTO getEndereco() {
        return endereco;
    }

    public void setEndereco(EnderecoDTO endereco) {
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
