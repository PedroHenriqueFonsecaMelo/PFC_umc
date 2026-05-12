package umc.exs.dtos.cancelamento;

import java.time.LocalDateTime;

import umc.exs.model.enums.MotivoCategoria;
import umc.exs.model.enums.StatusSolicitacao;

public class SolicitacaoCancelamentoResponseDTO {

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

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public String getTituloLivro() { return tituloLivro; }
    public void setTituloLivro(String tituloLivro) { this.tituloLivro = tituloLivro; }

    public String getAutorLivro() { return autorLivro; }
    public void setAutorLivro(String autorLivro) { this.autorLivro = autorLivro; }

    public String getFotosUrls() { return fotosUrls; }
    public void setFotosUrls(String fotosUrls) { this.fotosUrls = fotosUrls; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

    public MotivoCategoria getMotivoCategoria() { return motivoCategoria; }
    public void setMotivoCategoria(MotivoCategoria motivoCategoria) { this.motivoCategoria = motivoCategoria; }

    public String getMotivoCategoriaDescricao() { return motivoCategoriaDescricao; }
    public void setMotivoCategoriaDescricao(String motivoCategoriaDescricao) { this.motivoCategoriaDescricao = motivoCategoriaDescricao; }

    public String getMotivoDescricao() { return motivoDescricao; }
    public void setMotivoDescricao(String motivoDescricao) { this.motivoDescricao = motivoDescricao; }

    public StatusSolicitacao getStatus() { return status; }
    public void setStatus(StatusSolicitacao status) { this.status = status; }

    public String getStatusDescricao() { return statusDescricao; }
    public void setStatusDescricao(String statusDescricao) { this.statusDescricao = statusDescricao; }

    public String getComentarioAdmin() { return comentarioAdmin; }
    public void setComentarioAdmin(String comentarioAdmin) { this.comentarioAdmin = comentarioAdmin; }

    public LocalDateTime getDataSolicitacao() { return dataSolicitacao; }
    public void setDataSolicitacao(LocalDateTime dataSolicitacao) { this.dataSolicitacao = dataSolicitacao; }

    public LocalDateTime getDataResposta() { return dataResposta; }
    public void setDataResposta(LocalDateTime dataResposta) { this.dataResposta = dataResposta; }

    public Double getPrecoLivro() { return precoLivro; }
    public void setPrecoLivro(Double precoLivro) { this.precoLivro = precoLivro; }

    public String getIsbnLivro() { return isbnLivro; }
    public void setIsbnLivro(String isbnLivro) { this.isbnLivro = isbnLivro; }

    public LocalDateTime getDataCompra() { return dataCompra; }
    public void setDataCompra(LocalDateTime dataCompra) { this.dataCompra = dataCompra; }

    public Double getSaldoAtualComprador() { return saldoAtualComprador; }
    public void setSaldoAtualComprador(Double saldoAtualComprador) { this.saldoAtualComprador = saldoAtualComprador; }
}
