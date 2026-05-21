package umc.exs.dto.response.compras;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusSolicitacao;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CancelamentoResponse {

    private Long id;
    private Long pedidoId;
    private String tituloLivro;
    private String autorLivro;
    private String fotosUrls;
    private String clienteNome;
    private String clienteEmail;
    private MotivoCategoria motivoCategoria;
    private String motivoCategoriaDescricao;
    private String motivoDescricao;
    private StatusSolicitacao status;
    private String statusDescricao;
    private String comentarioAdmin;
    private LocalDateTime dataSolicitacao;
    private LocalDateTime dataResposta;
    private Double precoLivro;
    private String isbnLivro;
    private LocalDateTime dataCompra;
    private Double saldoAtualComprador;
}
