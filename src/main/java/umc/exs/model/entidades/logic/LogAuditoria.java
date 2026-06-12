package umc.exs.model.entidades.logic;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Registra automaticamente as ações dos usuários na plataforma para fins de
 * auditoria e conformidade com a LGPD, com 28 tipos de eventos rastreados.
 */
@Entity
@Table(name = "log_auditoria")

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID do usuário alvo da ação
    @Column(name = "id_usuario", nullable = true)
    private Long idUsuario;

    // Email do usuário (para referência rápida no banco)
    @Column(name = "email_usuario", length = 100, nullable = true)
    private String emailUsuario;

    // Tipo de ação realizada (ex: LOGIN_SUCESSO)
    @Column(name = "acao", length = 50, nullable = false)
    private String acao;

    // Mensagem detalhada sobre a ação
    @Column(name = "detalhes", columnDefinition = "TEXT")
    private String detalhes;

    // Data e hora da ocorrência do log
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    public LogAuditoria(Long idUsuario, String emailUsuario, String acao, String detalhes, LocalDateTime dataHora) {
        this.idUsuario = idUsuario;
        this.emailUsuario = emailUsuario;
        this.acao = acao;
        this.detalhes = detalhes;
        this.dataHora = dataHora;
    }

    /** Retorna o nome legível da ação para exibição no frontend/Thymeleaf. */
    public String getAcaoDescricao() {
        if (acao == null)
            return "—";
        switch (acao) {
            case "LOGIN_SUCESSO":
                return "Login realizado";
            case "AUTENT_FALHA":
                return "Falha na autenticação";
            case "ALTERACAO_SENHA":
            case "SENHA_ALTERADA":
                return "Senha alterada";
            case "CADASTRO_USUARIO":
                return "Cadastro iniciado";
            case "CADASTRO_COMPLETO":
                return "Cadastro concluído";
            case "UPLOAD_FOTO":
                return "Foto de perfil atualizada";
            case "COMPRA_LIVRO_SUCESSO":
                return "Compra realizada";
            case "COMPRA_CARRINHO":
                return "Compra via carrinho";
            case "PEDIDO_STATUS_ATUALIZADO":
                return "Status do pedido atualizado";
            case "PEDIDO_CANCELADO_ESTORNO":
                return "Pedido cancelado com estorno";
            case "CANCELAMENTO_SOLICITADO":
                return "Cancelamento solicitado";
            case "CANCELAMENTO_APROVADO":
                return "Cancelamento aprovado";
            case "CANCELAMENTO_RECUSADO":
                return "Cancelamento recusado";
            case "CANCELAMENTO_ADMIN":
                return "Cancelamento pelo administrador";
            case "LIVRO_CADASTRADO":
                return "Livro cadastrado";
            case "LOTE_CADASTRADO":
                return "Lote cadastrado";
            case "LIVRO_APROVADO":
                return "Livro aprovado";
            case "LIVRO_REJEITADO":
                return "Livro rejeitado";
            case "TOKENS_ADICIONADOS":
                return "Tokens adicionados";
            case "TOKENS_DEBITADOS":
                return "Tokens debitados";
            case "RECARGA_TOKENS":
                return "Recarga de tokens";
            case "PIX_FALHA":
                return "Falha no pagamento via Pix";
            case "NIVEL_SUBIU":
                return "Nível aumentado";
            case "XP_ZERADO":
                return "XP zerado por inatividade";
            case "ERRO_CUPOM":
                return "Erro ao aplicar cupom";
            case "EXPORTACAO_CSV":
                return "Exportação CSV";
            case "EXPORTACAO_PDF":
                return "Exportação PDF";
            default:
                return acao.replace('_', ' ').toLowerCase();
        }
    }

    public LogAuditoria(String acao2, String detalhes2, LocalDateTime agora) {
        this.acao = acao2;
        this.detalhes = detalhes2;
        this.dataHora = agora;
    }

    public LogAuditoria(String emailUsuario2, String acao2, String detalhes2, LocalDateTime agora) {
        this.emailUsuario = emailUsuario2;
        this.acao = acao2;
        this.detalhes = detalhes2;
        this.dataHora = agora;
    }

}
