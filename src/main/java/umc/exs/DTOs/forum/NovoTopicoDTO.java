package umc.exs.DTOs.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.enums.CategoriaForum;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NovoTopicoDTO {

    @NotBlank(message = "O título é obrigatório.")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres.")
    private String titulo;

    @NotBlank(message = "O conteúdo é obrigatório.")
    private String conteudo;

    @NotNull(message = "Selecione uma categoria.")
    private CategoriaForum categoria;
}
