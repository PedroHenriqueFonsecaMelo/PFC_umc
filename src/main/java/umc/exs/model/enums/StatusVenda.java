package umc.exs.model.enums;

/**
 * Define os estados do ciclo de vida de um livro anunciado pelo vendedor na
 * plataforma.
 */
public enum StatusVenda {
    // Livro submetido aguardando análise do admin.
    AGUARDANDO_APROVACAO,
    // Livro aprovado e disponível para compra.
    NA_VITRINE,
    // Livro comprado por um cliente.
    VENDIDO,
    // Livro reprovado pelo admin com motivo informado.
    REJEITADO
}
