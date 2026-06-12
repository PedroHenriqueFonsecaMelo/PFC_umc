package umc.exs.model.enums;

/**
 * Define o estado da conta do cliente na plataforma, controlando o acesso ao
 * sistema.
 */
public enum StatusConta {
    // Conta normal com acesso completo.
    ATIVO,
    // Conta temporariamente bloqueada pelo admin.
    SUSPENSO,
    // Conta permanentemente desativada (soft delete).
    REMOVIDO
}
