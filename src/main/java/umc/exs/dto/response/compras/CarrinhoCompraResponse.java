package umc.exs.dto.response.compras;

import lombok.NoArgsConstructor;
import umc.exs.dto.extern.ItemResultadoDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO de resposta da compra do carrinho.
 * Informa quais livros foram comprados, quais falharam e o saldo restante.
 */
@NoArgsConstructor
public class CarrinhoCompraResponse {

    private int totalSolicitados;
    private int totalComprados;
    private double totalGasto;
    private double saldoRestante;
    private double totalOriginal;
    private double descontoAplicado;
    private String codigoCupomAplicado;

    private List<ItemResultadoDTO> comprados;
    private List<ItemResultadoDTO> falhas;

    /**
     * Inicializa o DTO com todos os campos, aplicando cópias defensivas nas listas.
     * Garante que modificações externas nas listas originais não afetem este objeto.
     */
    private CarrinhoCompraResponse(
            int totalSolicitados,
            int totalComprados,
            double totalGasto,
            double saldoRestante,
            double totalOriginal,
            double descontoAplicado,
            String codigoCupomAplicado,
            List<ItemResultadoDTO> comprados,
            List<ItemResultadoDTO> falhas) {

        this.totalSolicitados = totalSolicitados;
        this.totalComprados = totalComprados;
        this.totalGasto = totalGasto;
        this.saldoRestante = saldoRestante;
        this.totalOriginal = totalOriginal;
        this.descontoAplicado = descontoAplicado;
        this.codigoCupomAplicado = codigoCupomAplicado;

        // cópia defensiva SEM null
        this.comprados = (comprados == null) ? new ArrayList<>() : new ArrayList<>(comprados);
        this.falhas = (falhas == null) ? new ArrayList<>() : new ArrayList<>(falhas);
    }

    // ========================
    // GETTERS SEGUROS
    // ========================

    public int getTotalSolicitados() {
        return totalSolicitados;
    }

    public int getTotalComprados() {
        return totalComprados;
    }

    public double getTotalGasto() {
        return totalGasto;
    }

    public double getSaldoRestante() {
        return saldoRestante;
    }

    public double getTotalOriginal() {
        return totalOriginal;
    }

    public double getDescontoAplicado() {
        return descontoAplicado;
    }

    public String getCodigoCupomAplicado() {
        return codigoCupomAplicado;
    }

    /**
     * Retorna lista imutável dos livros comprados com sucesso nesta operação de carrinho.
     * A imutabilidade evita que código externo adicione ou remova itens indevidamente.
     */
    public List<ItemResultadoDTO> getComprados() {
        return Collections.unmodifiableList(comprados);
    }

    /**
     * Retorna lista imutável dos livros que falharam na compra, com o motivo de cada falha.
     * A imutabilidade evita que código externo adicione ou remova itens indevidamente.
     */
    public List<ItemResultadoDTO> getFalhas() {
        return Collections.unmodifiableList(falhas);
    }

    // ========================
    // SETTERS COM DEFESA
    // ========================

    /**
     * Define a lista de comprados com cópia defensiva, substituindo null por lista vazia.
     * Nunca aceita referência direta à lista externa para manter o encapsulamento.
     */
    public void setComprados(List<ItemResultadoDTO> comprados) {
        this.comprados = (comprados == null) ? new ArrayList<>() : new ArrayList<>(comprados);
    }

    /**
     * Define a lista de falhas com cópia defensiva, substituindo null por lista vazia.
     * Nunca aceita referência direta à lista externa para manter o encapsulamento.
     */
    public void setFalhas(List<ItemResultadoDTO> falhas) {
        this.falhas = (falhas == null) ? new ArrayList<>() : new ArrayList<>(falhas);
    }

    // ========================
    // BUILDER MANUAL
    // ========================

    /**
     * Retorna o builder manual para construção fluente do CarrinhoCompraResponse.
     * Ponto de entrada para o padrão Builder implementado pela classe interna Builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Implementa o padrão Builder para criação segura e fluente do CarrinhoCompraResponse.
     * Aplica cópias defensivas nas listas durante a construção para garantir imutabilidade.
     */
    public static class Builder {
        private int totalSolicitados;
        private int totalComprados;
        private double totalGasto;
        private double saldoRestante;
        private double totalOriginal;
        private double descontoAplicado;
        private String codigoCupomAplicado;
        private List<ItemResultadoDTO> comprados = new ArrayList<>();
        private List<ItemResultadoDTO> falhas = new ArrayList<>();

        public Builder totalSolicitados(int totalSolicitados) {
            this.totalSolicitados = totalSolicitados;
            return this;
        }

        public Builder totalComprados(int totalComprados) {
            this.totalComprados = totalComprados;
            return this;
        }

        public Builder totalGasto(double totalGasto) {
            this.totalGasto = totalGasto;
            return this;
        }

        public Builder saldoRestante(double saldoRestante) {
            this.saldoRestante = saldoRestante;
            return this;
        }

        public Builder totalOriginal(double totalOriginal) {
            this.totalOriginal = totalOriginal;
            return this;
        }

        public Builder descontoAplicado(double descontoAplicado) {
            this.descontoAplicado = descontoAplicado;
            return this;
        }

        public Builder codigoCupomAplicado(String codigoCupomAplicado) {
            this.codigoCupomAplicado = codigoCupomAplicado;
            return this;
        }

        public Builder comprados(List<ItemResultadoDTO> comprados) {
            this.comprados = (comprados == null) ? new ArrayList<>() : new ArrayList<>(comprados);
            return this;
        }

        public Builder falhas(List<ItemResultadoDTO> falhas) {
            this.falhas = (falhas == null) ? new ArrayList<>() : new ArrayList<>(falhas);
            return this;
        }

        public CarrinhoCompraResponse build() {
            return new CarrinhoCompraResponse(
                    totalSolicitados,
                    totalComprados,
                    totalGasto,
                    saldoRestante,
                    totalOriginal,
                    descontoAplicado,
                    codigoCupomAplicado,
                    comprados,
                    falhas);
        }
    }

    // ========================
    // CLASSE INTERNA
    // ========================
}