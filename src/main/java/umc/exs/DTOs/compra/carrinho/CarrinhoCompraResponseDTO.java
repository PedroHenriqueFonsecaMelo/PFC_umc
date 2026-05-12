package umc.exs.dtos.compra.carrinho;


import lombok.NoArgsConstructor;
import umc.exs.dtos.compra.ItemResultadoDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * DTO de resposta da compra do carrinho.
 * Informa quais livros foram comprados, quais falharam e o saldo restante.
 */
@NoArgsConstructor
public class CarrinhoCompraResponseDTO {

    private int totalSolicitados;
    private int totalComprados;
    private double totalGasto;
    private double saldoRestante;
    private double totalOriginal;
    private double descontoAplicado;
    private String codigoCupomAplicado;

    private List<ItemResultadoDTO> comprados;
    private List<ItemResultadoDTO> falhas;

    private CarrinhoCompraResponseDTO(
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

        // ✔ cópia defensiva SEM null
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

    public List<ItemResultadoDTO> getComprados() {
        return Collections.unmodifiableList(comprados);
    }

    public List<ItemResultadoDTO> getFalhas() {
        return Collections.unmodifiableList(falhas);
    }

    // ========================
    // SETTERS COM DEFESA
    // ========================

    public void setComprados(List<ItemResultadoDTO> comprados) {
        this.comprados = (comprados == null) ? new ArrayList<>() : new ArrayList<>(comprados);
    }

    public void setFalhas(List<ItemResultadoDTO> falhas) {
        this.falhas = (falhas == null) ? new ArrayList<>() : new ArrayList<>(falhas);
    }

    // ========================
    // BUILDER MANUAL
    // ========================

    public static Builder builder() {
        return new Builder();
    }

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

        public CarrinhoCompraResponseDTO build() {
            return new CarrinhoCompraResponseDTO(
                    totalSolicitados,
                    totalComprados,
                    totalGasto,
                    saldoRestante,
                    totalOriginal,
                    descontoAplicado,
                    codigoCupomAplicado,
                    comprados,
                    falhas
            );
        }
    }

    // ========================
    // CLASSE INTERNA
    // ========================
}