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

/**
 * DTO usado pelo admin para cadastrar ou editar um livro diretamente no estoque.
 * Não passa pelo fluxo de aprovação do vendedor — o livro é inserido diretamente como aprovado.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class LivroAdminRequest {

    // Título do livro; obrigatório, entre 3 e 255 caracteres
    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 255, message = "O título deve ter entre 3 e 255 caracteres")
    String titulo;

    // Nome do autor; obrigatório, entre 3 e 255 caracteres
    @NotBlank(message = "O autor é obrigatório")
    @Size(min = 3, max = 255, message = "O autor deve ter entre 3 e 255 caracteres")
    String autor;

    // ISBN do livro; obrigatório, entre 10 e 20 caracteres
    @NotBlank(message = "O ISBN é obrigatório")
    @Size(min = 10, max = 20, message = "O ISBN deve ter entre 10 e 20 caracteres")
    String isbn;

    // Preço em tokens definido pelo admin; obrigatório e positivo
    @Positive(message = "A quantidade deve ser positiva")
    @NotNull(message = "O preço é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    Double preco;

    // Estado de conservação do exemplar (ex: NOVO, SEMINOVO, USADO); obrigatório
    @NotNull(message = "O estado do livro é obrigatório")
    EstadoLivro estado;

    // Descrição do livro; opcional, máximo 1000 caracteres
    @Size(max = 1000, message = "O resumo não pode ter mais de 1000 caracteres")
    String resumo;

    // URL da imagem de capa do livro; opcional
    private String capa;

    // ID do admin responsável pelo cadastro do livro
    Long adminId;

    // ID do vendedor associado ao livro, se houver
    Long vendedorId;

    // Gênero literário do livro; opcional, máximo 100 caracteres
    @Size(max = 100, message = "O gênero não pode ter mais de 100 caracteres")
    String genero;

    // Indica se o livro está em promoção; exibe badge e preço riscado na vitrine
    Boolean emPromocao;

    // Percentual de desconto aplicado ao preço quando em promoção; entre 0 e 100%
    @Min(value = 0, message = "O desconto não pode ser negativo")
    @Max(value = 100, message = "O desconto não pode ser maior que 100%")
    Double percentualDesconto;

    // Data e hora de expiração da promoção no formato yyyy-MM-dd'T'HH:mm:ss
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime promocaoExpira;
}
