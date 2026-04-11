package umc.exs.DTOs.forum;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NovaRespostaDTO {

    @NotBlank(message = "A resposta não pode estar vazia.")
    private String conteudo;
}
