package umc.exs.dto.request.admin;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LivroAdminRequest {
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 255, message = "O título deve ter entre 3 e 255 caracteres")
    String titulo;

    @NotBlank(message = "O autor é obrigatório")
    @Size(min = 3, max = 255, message = "O autor deve ter entre 3 e 255 caracteres")
    String autor;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(min = 10, max = 20, message = "O ISBN deve ter entre 10 e 20 caracteres")
    String isbn;

    @Positive(message = "A quantidade deve ser positiva")
    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    Double preco;

    @NotNull(message = "O estado do livro é obrigatório")
    EstadoLivro estado;

    @Size(max = 1000, message = "O resumo não pode ter mais de 1000 caracteres")
    String resumo;

    private String capa;

    Long adminId;
    Long vendedorId;

    @Size(max = 100, message = "O gênero não pode ter mais de 100 caracteres")
    String genero;

    Boolean emPromocao;

    @Min(value = 0, message = "O desconto não pode ser negativo")
    @Max(value = 100, message = "O desconto não pode ser maior que 100%")
    Double percentualDesconto;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime promocaoExpira;
}