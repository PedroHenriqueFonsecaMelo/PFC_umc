package umc.exs.dto.request.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.enums.CategoriaForum;

/**
 * DTO enviado pelo cliente ao criar um novo tópico no fórum.
 * Contém título, conteúdo e a categoria em que o tópico será publicado.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NovoTopicoRequest {

    // Título do tópico; obrigatório, máximo 200 caracteres
    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres.")
    private String titulo;

    // Texto do tópico escrito pelo cliente; obrigatório
    @NotBlank(message = "O conteúdo é obrigatório.")
    private String conteudo;

    // Categoria do fórum (ex: Resenhas, Dúvidas, Recomendações, Geral); obrigatória
    @NotNull(message = "Selecione uma categoria.")
    private CategoriaForum categoria;
}
