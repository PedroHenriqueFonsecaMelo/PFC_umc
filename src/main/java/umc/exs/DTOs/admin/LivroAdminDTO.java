package umc.exs.dtos.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivroAdminDTO {
    private String titulo;
    private String autor;
    private String isbn;
    private String estado;
    private Double preco;
    private String resumo;
}

