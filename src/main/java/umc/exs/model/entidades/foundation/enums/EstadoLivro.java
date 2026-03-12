package umc.exs.model.entidades.foundation.enums;

public enum EstadoLivro {
    NOVO(50), 
    OTIMO(40), 
    BOM(30), 
    DESGASTADO(20), 
    RUIM(0);

    private final int preco;

    EstadoLivro(int preco) {
        this.preco = preco;
    }

    public int getPreco() {
        return preco;
    }
}
