package umc.exs.model.enums;

/**
 * Define os estados de conservação de um livro e o preço em tokens atribuído
 * automaticamente pelo sistema na aprovação: NOVO=50, ÓTIMO=40, BOM=30,
 * DESGASTADO=20, RUIM=0.
 */
public enum EstadoLivro {
    // Livro novo, sem uso — 50 tokens.
    NOVO(50),
    // Livro em ótimo estado, com uso mínimo — 40 tokens.
    OTIMO(40),
    // Livro em bom estado, com uso moderado — 30 tokens.
    BOM(30),
    // Livro desgastado, com sinais visíveis de uso — 20 tokens.
    DESGASTADO(20),
    // Livro em mau estado, sem valor de tokens — 0 tokens.
    RUIM(0);

    private final int preco;

    EstadoLivro(int preco) {
        this.preco = preco;
    }

    /** Retorna o preço em tokens definido para o estado de conservação. */
    public int getPreco() {
        return preco;
    }
}
