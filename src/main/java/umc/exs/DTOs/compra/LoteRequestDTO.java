package umc.exs.DTOs.compra;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.DTOs.livro.LivroItemDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequestDTO {
    private List<LivroItemDTO> livros;
}
