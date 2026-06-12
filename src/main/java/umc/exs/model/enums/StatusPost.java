package umc.exs.model.enums;

/**
 * Define os estados do ciclo de vida de um post do blog da plataforma.
 */
public enum StatusPost {
    // Post em criação, visível apenas pelo admin.
    RASCUNHO,
    // Post submetido para aprovação antes da publicação.
    EM_REVISAO,
    // Post visível para todos os usuários.
    PUBLICADO,
    // Post programado para publicação em data futura.
    AGENDADO
}
