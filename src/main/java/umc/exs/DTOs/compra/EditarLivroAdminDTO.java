package umc.exs.dtos.compra;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.EstadoLivro;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EditarLivroAdminDTO {
    public Long id;
    public String titulo;
    public String autor;
    public String isbn;
    public Double preco;
    public EstadoLivro estado;
    public String resumo;
    public Boolean emPromocao;
    public Double percentualDesconto;
    public LocalDateTime promocaoExpira;
    public String capa;
}