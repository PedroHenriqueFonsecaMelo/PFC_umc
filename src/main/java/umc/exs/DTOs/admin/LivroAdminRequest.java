package umc.exs.dtos.admin;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LivroAdminRequest {
    String titulo;
    String autor;
    String isbn;
    Double preco;
    EstadoLivro estado;
    String resumo;
    String capa;
    Long adminId;
    Long vendedorId;
    String genero;
    Boolean emPromocao;
    Double percentualDesconto;
    LocalDateTime promocaoExpira;
}