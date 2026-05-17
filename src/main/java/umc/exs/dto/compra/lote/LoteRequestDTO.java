package umc.exs.dto.compra.lote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.dto.livro.LivroItemDTO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequestDTO {
    private List<LivroItemDTO> livros;
}
