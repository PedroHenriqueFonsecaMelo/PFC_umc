package umc.exs.model.dtos.auth;

public class LogDTO {
    private String acao;
    private Long idUsuario;
    private String detalhes;
    private String timestamp;

    // Construtor, Getters e Setters
    public LogDTO(String acao, Long idUsuario, String detalhes, String timestamp) {
        this.acao = acao;
        this.idUsuario = idUsuario;
        this.detalhes = detalhes;
        this.timestamp = timestamp;
    }
    
    // Inclua o construtor vazio, getters e setters completos
    public LogDTO() {}
    
    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public Long getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Long idUsuario) { this.idUsuario = idUsuario; }

    public String getDetalhes() { return detalhes; }
    public void setDetaihes(String detalhes) { this.detalhes = detalhes; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
