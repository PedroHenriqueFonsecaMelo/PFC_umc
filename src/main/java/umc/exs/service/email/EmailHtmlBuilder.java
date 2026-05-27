package umc.exs.service.email;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gera corpos HTML para todos os e-mails transacionais da Bibliotroca.
 * Use CSS inline — sem folhas externas — para máxima compatibilidade com
 * clientes de e-mail.
 */
public final class EmailHtmlBuilder {

    private EmailHtmlBuilder() {
    }

    // ── Paleta ────────────────────────────────────────────────────────────────
    private static final String COR_PRIMARIA = "#722F37";
    private static final String COR_OURO = "#c9a96e";
    private static final String COR_FUNDO = "#f9f6f0";
    private static final String COR_TEXTO = "#2c241b";
    private static final String COR_MUTED = "#7a6e65";
    private static final String COR_SUCESSO = "#2e7d32";
    private static final String COR_ALERTA = "#b45309";

    // ── Base HTML ─────────────────────────────────────────────────────────────

    private static String base(String titulo, String corpo) {
        return "<!DOCTYPE html>" +
                "<html lang='pt-BR'><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "<title>" + titulo + "</title></head>" +
                "<body style='margin:0;padding:0;background:" + COR_FUNDO
                + ";font-family:Arial,Helvetica,sans-serif;color:" + COR_TEXTO + ";'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='background:" + COR_FUNDO
                + ";'><tr><td align='center' style='padding:32px 16px;'>" +
                "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;background:#fff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,.08);'>"
                +

                // Cabeçalho
                "<tr><td style='background:" + COR_PRIMARIA + ";padding:28px 40px;text-align:center;'>" +
                "<h1 style='margin:0;font-size:26px;color:#fff;letter-spacing:2px;font-family:Georgia,serif;'>Bibliotroca</h1>"
                +
                "<p style='margin:4px 0 0;font-size:13px;color:" + COR_OURO
                + ";letter-spacing:1px;'>sua biblioteca de trocas</p>" +
                "</td></tr>" +

                // Corpo
                "<tr><td style='padding:36px 40px;'>" + corpo + "</td></tr>" +

                // Rodapé
                "<tr><td style='background:#f3ede6;padding:20px 40px;text-align:center;border-top:1px solid #e8ddd4;'>"
                +
                "<p style='margin:0;font-size:12px;color:" + COR_MUTED
                + ";'>Este e-mail foi enviado automaticamente. Por favor, não responda.</p>" +
                "<p style='margin:6px 0 0;font-size:13px;color:" + COR_PRIMARIA
                + ";font-weight:bold;'>Equipe Bibliotroca</p>" +
                "</td></tr>" +

                "</table></td></tr></table>" +
                "</body></html>";
    }

    private static String saudacao(String nome) {
        return "<h2 style='margin:0 0 16px;font-size:18px;color:" + COR_PRIMARIA + ";font-family:Georgia,serif;'>Olá, "
                + nome + "!</h2>";
    }

    private static String paragrafo(String texto) {
        return "<p style='margin:0 0 14px;font-size:15px;line-height:1.6;color:" + COR_TEXTO + ";'>" + texto + "</p>";
    }

    private static String botao(String texto, String url) {
        return "<div style='text-align:center;margin:24px 0;'>" +
                "<a href='" + url + "' style='display:inline-block;padding:13px 32px;background:" + COR_PRIMARIA
                + ";color:#fff;" +
                "text-decoration:none;border-radius:6px;font-size:15px;font-weight:bold;letter-spacing:.5px;'>" + texto
                + "</a>" +
                "</div>";
    }

    private static String aviso(String texto) {
        return "<div style='background:#fff8f0;border-left:4px solid " + COR_ALERTA
                + ";padding:12px 16px;margin:16px 0;border-radius:0 4px 4px 0;'>" +
                "<p style='margin:0;font-size:13px;color:" + COR_ALERTA + ";'>" + texto + "</p>" +
                "</div>";
    }

    private static String linhaSaldo(String label, String valor, String cor) {
        return "<tr>" +
                "<td style='padding:8px 16px;font-size:14px;color:" + COR_MUTED + ";border-bottom:1px solid #f0e8e0;'>"
                + label + "</td>" +
                "<td style='padding:8px 16px;font-size:14px;font-weight:bold;color:" + cor
                + ";text-align:right;border-bottom:1px solid #f0e8e0;'>" + valor + "</td>" +
                "</tr>";
    }

    private static String tabelaFinanceira(String... linhas) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<table width='100%' cellpadding='0' cellspacing='0' style='margin:16px 0;border:1px solid #e8ddd4;border-radius:6px;overflow:hidden;'>");
        for (String l : linhas)
            sb.append(l);
        sb.append("</table>");
        return sb.toString();
    }

    private static String divisor() {
        return "<hr style='border:none;border-top:1px solid #e8ddd4;margin:20px 0;'/>";
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    /** Confirmação de e-mail após cadastro. */
    public static String verificacaoEmail(String nome, String link) {
        String corpo = saudacao(nome) +
                paragrafo(
                        "Obrigado por se cadastrar na <strong>Bibliotroca</strong>! Estamos animados em ter você na nossa comunidade de leitores.")
                +
                paragrafo("Para ativar sua conta, confirme seu endereço de e-mail clicando no botão abaixo:") +
                botao("Confirmar meu e-mail", link) +
                aviso("Este link expira em <strong>24 horas</strong>. Se você não criou uma conta na Bibliotroca, ignore este e-mail.")
                +
                paragrafo("<small style='color:" + COR_MUTED
                        + ";'>Se o botão não funcionar, copie e cole o link no seu navegador:<br><a href='" + link
                        + "' style='color:" + COR_PRIMARIA + ";'>" + link + "</a></small>");
        return base("Confirme seu e-mail — Bibliotroca", corpo);
    }

    /** Link de recuperação de senha. */
    public static String recuperacaoSenha(String nome, String link) {
        String corpo = saudacao(nome) +
                paragrafo(
                        "Recebemos uma solicitação para redefinir a senha da sua conta na <strong>Bibliotroca</strong>.")
                +
                paragrafo("Clique no botão abaixo para criar uma nova senha:") +
                botao("Redefinir minha senha", link) +
                aviso("Este link é válido por <strong>30 minutos</strong>. Se você não solicitou a redefinição, ignore este e-mail — sua senha permanece a mesma.")
                +
                paragrafo("<small style='color:" + COR_MUTED
                        + ";'>Se o botão não funcionar, copie e cole o link no seu navegador:<br><a href='" + link
                        + "' style='color:" + COR_PRIMARIA + ";'>" + link + "</a></small>");
        return base("Redefinição de senha — Bibliotroca", corpo);
    }

    /** Confirmação de que a senha foi alterada com sucesso. */
    public static String senhaRedefinida(String nome, String dataHora) {
        String corpo = saudacao(nome) +
                paragrafo("Sua senha foi <strong style='color:" + COR_SUCESSO + ";'>redefinida com sucesso</strong>.") +
                tabelaFinanceira(
                        linhaSaldo("Data e hora", dataHora, COR_TEXTO))
                +
                paragrafo("Se você realizou essa alteração, pode ignorar este e-mail.") +
                aviso("<strong>Não foi você?</strong> Se você não reconhece essa atividade, sua conta pode estar comprometida. Entre em contato imediatamente pelo e-mail <strong>bibliotroca.noreply@gmail.com</strong>.");
        return base("Sua senha foi redefinida — Bibliotroca", corpo);
    }

    /** Compra de livro individual. */
    public static String compraSucesso(String nome, String tituloLivro, double preco, double saldo, String baseUrl) {
        String corpo = saudacao(nome) +
                paragrafo("Sua compra foi <strong style='color:" + COR_SUCESSO
                        + ";'>confirmada com sucesso</strong>! Seu livro está a caminho.")
                +
                "<h3 style='margin:20px 0 8px;font-size:15px;color:" + COR_MUTED
                + ";text-transform:uppercase;letter-spacing:.05em;'>Detalhe da compra</h3>" +
                tabelaFinanceira(
                        linhaSaldo("Livro adquirido", tituloLivro, COR_TEXTO),
                        linhaSaldo("Valor debitado", String.format("T$ %.2f", preco), COR_PRIMARIA),
                        linhaSaldo("Saldo restante", String.format("T$ %.2f", saldo), COR_SUCESSO))
                +
                botao("Acompanhar meus pedidos", baseUrl + "/clientes/minhas-compras");
        return base("Compra realizada com sucesso! — Bibliotroca", corpo);
    }

    /** Compra em lote (carrinho). Recebe lista de pares título/preço. */
    public static String carrinhoConfirmado(String nome, List<String[]> itens, double total, double saldo,
            String baseUrl) {
        StringBuilder linhasItens = new StringBuilder();
        for (String[] item : itens) {
            linhasItens.append(linhaSaldo(item[0], "T$ " + item[1], COR_TEXTO));
        }
        linhasItens.append(linhaSaldo("Total debitado", String.format("T$ %.2f", total), COR_PRIMARIA));
        linhasItens.append(linhaSaldo("Saldo restante", String.format("T$ %.2f", saldo), COR_SUCESSO));

        String corpo = saudacao(nome) +
                paragrafo("Sua compra foi <strong style='color:" + COR_SUCESSO
                        + ";'>confirmada com sucesso</strong>! Confira os itens abaixo.")
                +
                "<h3 style='margin:20px 0 8px;font-size:15px;color:" + COR_MUTED
                + ";text-transform:uppercase;letter-spacing:.05em;'>Itens adquiridos</h3>" +
                tabelaFinanceira(linhasItens.toString()) +
                botao("Acompanhar meus pedidos", baseUrl + "/clientes/minhas-compras");
        return base("Compra do carrinho confirmada! — Bibliotroca", corpo);
    }

    /**
     * Notificação genérica de atualização de saldo — crédito ou débito.
     *
     * @param nome          Nome do cliente
     * @param saldoAnterior Saldo antes da operação
     * @param movimentacao  Valor absoluto da movimentação
     * @param saldoAtual    Saldo após a operação
     * @param motivo        Descrição curta (ex.: "Resgate de cupom XP-A1B2",
     *                      "Estorno — pedido #42")
     * @param ehCredito     true = crédito (verde/+), false = débito (vermelho/−)
     * @param dataHora      Momento em que a operação ocorreu
     */
    public static String atualizacaoSaldo(String nome, double saldoAnterior, double movimentacao,
            double saldoAtual, String motivo, boolean ehCredito,
            LocalDateTime dataHora) {
        String corMov = ehCredito ? COR_SUCESSO : COR_PRIMARIA;
        String sinal = ehCredito ? "+" : "−";
        String tituloCor = ehCredito ? COR_SUCESSO : COR_PRIMARIA;
        String tituloTxt = ehCredito ? "Crédito de tokens" : "Débito de tokens";
        String dataFmt = dataHora != null
                ? dataHora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : "—";

        String corpo = saudacao(nome) +
                paragrafo("Seu saldo de tokens foi atualizado. Veja os detalhes abaixo.") +
                "<h3 style='margin:20px 0 8px;font-size:15px;color:" + tituloCor + ";text-transform:uppercase;" +
                "letter-spacing:.05em;'>" + tituloTxt + "</h3>" +
                tabelaFinanceira(
                        linhaSaldo("Tipo de movimentação", motivo, COR_TEXTO),
                        linhaSaldo("Saldo anterior", String.format("T$ %.2f", saldoAnterior), COR_MUTED),
                        linhaSaldo("Movimentação", sinal + String.format(" T$ %.2f", movimentacao), corMov),
                        linhaSaldo("Saldo atual", String.format("T$ %.2f", saldoAtual), COR_PRIMARIA),
                        linhaSaldo("Data e hora", dataFmt, COR_MUTED))
                +
                paragrafo("Qualquer dúvida, entre em contato pelo e-mail " +
                        "<strong>bibliotroca.noreply@gmail.com</strong>.");
        return base("Atualização de saldo — Bibliotroca", corpo);
    }

    /** Confirmação de recarga de tokens na carteira. */
    public static String recargaTokens(String nome, double valor, String metodo, double saldoAnterior,
            double saldoAtual) {
        String corpo = saudacao(nome) +
                paragrafo("Sua recarga de tokens foi <strong style='color:" + COR_SUCESSO
                        + ";'>processada com sucesso</strong>!")
                +
                "<h3 style='margin:20px 0 8px;font-size:15px;color:" + COR_MUTED
                + ";text-transform:uppercase;letter-spacing:.05em;'>Detalhes da recarga</h3>" +
                tabelaFinanceira(
                        linhaSaldo("Valor creditado", String.format("T$ %.2f", valor), COR_SUCESSO),
                        linhaSaldo("Método de pagamento", metodo, COR_TEXTO),
                        linhaSaldo("Saldo anterior", String.format("T$ %.2f", saldoAnterior), COR_MUTED),
                        linhaSaldo("Novo saldo", String.format("T$ %.2f", saldoAtual), COR_PRIMARIA))
                +
                paragrafo("Aproveite seus tokens para adquirir novos livros na vitrine!");
        return base("Recarga de tokens confirmada! — Bibliotroca", corpo);
    }

    /** Notificação ao vendedor de que seu livro foi aprovado. */
    public static String livroAprovado(String nome, String tituloLivro, double recompensa) {
        String corpo = saudacao(nome) +
                paragrafo("Ótima notícia! Seu livro foi <strong style='color:" + COR_SUCESSO
                        + ";'>aprovado</strong> e já está disponível na vitrine da Bibliotroca.")
                +
                tabelaFinanceira(
                        linhaSaldo("Livro aprovado", tituloLivro, COR_TEXTO),
                        linhaSaldo("Recompensa creditada", String.format("T$ %.2f", recompensa), COR_SUCESSO))
                +
                paragrafo("Obrigado por contribuir com nossa comunidade de leitores!");
        return base("Seu livro foi aprovado! — Bibliotroca", corpo);
    }

    /** Notificação ao vendedor de que seu livro foi rejeitado. */
    public static String livroRejeitado(String nome, String tituloLivro, String motivo) {
        String motivoHtml = (motivo != null && !motivo.isBlank())
                ? aviso("<strong>Motivo:</strong> " + motivo)
                : "";
        String corpo = saudacao(nome) +
                paragrafo("Infelizmente, o livro abaixo <strong>não foi aprovado</strong> desta vez.") +
                tabelaFinanceira(
                        linhaSaldo("Livro", tituloLivro, COR_TEXTO))
                +
                motivoHtml +
                paragrafo(
                        "Você pode enviar novos livros para avaliação a qualquer momento. Ficamos felizes em receber suas contribuições!");
        return base("Livro não aprovado — Bibliotroca", corpo);
    }

    /** Atualização de status de pedido. */
    public static String atualizacaoPedido(String nome, long pedidoId, String statusDescricao,
            String tituloLivro, String codigoRastreio,
            boolean cancelado, double precoCancelado,
            String baseUrl) {
        StringBuilder linhas = new StringBuilder();
        linhas.append(linhaSaldo("Pedido", "#" + pedidoId, COR_TEXTO));
        linhas.append(linhaSaldo("Livro", tituloLivro, COR_TEXTO));
        linhas.append(linhaSaldo("Novo status", statusDescricao, COR_PRIMARIA));
        if (codigoRastreio != null && !codigoRastreio.isBlank()) {
            linhas.append(linhaSaldo("Código de rastreio", codigoRastreio, COR_TEXTO));
        }
        if (cancelado) {
            linhas.append(linhaSaldo("Estorno", String.format("T$ %.2f devolvido ao seu saldo", precoCancelado),
                    COR_SUCESSO));
        }

        String corpo = saudacao(nome) +
                paragrafo("Há uma atualização no seu pedido:") +
                tabelaFinanceira(linhas.toString()) +
                botao("Ver meus pedidos", baseUrl + "/clientes/minhas-compras");
        return base("Atualização do pedido #" + pedidoId + " — Bibliotroca", corpo);
    }

    /** Notificação de livro da lista de desejos disponível. */
    public static String listaDesejosDisponivel(String nome, String tituloLivro, String isbn, boolean preReserva,
            String baseUrl) {
        String assuntoVisual = preReserva ? "Pré-reserva ativada!" : "Livro disponível!";
        String mensagem = preReserva
                ? "Sua <strong>pré-reserva está ativa</strong>. Acesse a vitrine agora para confirmar a compra e garantir seu exemplar!"
                : "Corra para garantir o seu exemplar antes que esgote!";
        String corpo = saudacao(nome) +
                paragrafo("<strong>" + assuntoVisual
                        + "</strong> Um livro da sua lista de desejos acaba de ficar disponível na vitrine.")
                +
                tabelaFinanceira(
                        linhaSaldo("Livro", tituloLivro, COR_TEXTO),
                        linhaSaldo("ISBN", isbn, COR_MUTED))
                +
                paragrafo(mensagem) +
                botao("Ver na vitrine", baseUrl + "/livros/vitrine");
        return base((preReserva ? "Pré-reserva ativada" : "Livro disponível") + " — Bibliotroca", corpo);
    }

    /** Aviso de cupom próximo ao vencimento. */
    public static String cupomExpirando(String nome, String codigo, double percentualDesconto, String dataExpiracao) {
        String corpo = saudacao(nome) +
                paragrafo("Seu cupom está prestes a vencer. Não deixe ele expirar!") +
                tabelaFinanceira(
                        linhaSaldo("Código do cupom", codigo, COR_PRIMARIA),
                        linhaSaldo("Desconto", String.format("%.0f%% de desconto", percentualDesconto), COR_SUCESSO),
                        linhaSaldo("Vencimento", dataExpiracao, COR_ALERTA))
                +
                aviso("Use seu cupom antes que expire!") +
                paragrafo("Aplique na vitrine da Bibliotroca durante o checkout e aproveite seu desconto.");
        return base("Seu cupom vai expirar em breve! — Bibliotroca", corpo);
    }

    /**
     * Comunicado em massa disparado pelo admin.
     * Converte quebras de linha do textarea em parágrafos HTML.
     */
    public static String comunicadoAdmin(String nome, String corpo) {
        StringBuilder paragrafos = new StringBuilder();
        paragrafos.append(saudacao(nome));
        for (String linha : corpo.split("\\n")) {
            String l = linha.trim();
            if (!l.isEmpty()) {
                paragrafos.append(paragrafo(l));
            }
        }
        paragrafos.append(
                "<p style='margin:24px 0 0;font-size:12px;color:" + COR_MUTED + ";border-top:1px solid #e8e0d5;" +
                        "padding-top:16px;'>Este é um comunicado oficial da Bibliotroca. " +
                        "Não responda a este e-mail.</p>");
        return base("Comunicado — Bibliotroca", paragrafos.toString());
    }

    /** Cancelamento de pedido APROVADO pelo admin (com estorno). */
    public static String cancelamentoAprovado(String nome, Long pedidoId, String tituloLivro,
            double valorEstorno, double novoSaldo, String comentarioAdmin) {
        String corpo = saudacao(nome) +
                paragrafo("Sua solicitação de cancelamento do pedido <strong>#" + pedidoId + "</strong> foi <strong>aprovada</strong>.") +
                tabelaFinanceira(
                        linhaSaldo("Livro", tituloLivro, COR_TEXTO),
                        linhaSaldo("Valor estornado", String.format("T$ %.2f", valorEstorno), COR_SUCESSO),
                        linhaSaldo("Novo saldo", String.format("T$ %.2f", novoSaldo), COR_PRIMARIA)) +
                (comentarioAdmin != null && !comentarioAdmin.isBlank()
                        ? aviso("Comentário da equipe: " + comentarioAdmin)
                        : "") +
                paragrafo("Os créditos foram devolvidos ao seu saldo e já estão disponíveis para uso.") +
                botao("Ver meu saldo", "/clientes/carteira");
        return base("Cancelamento aprovado — Bibliotroca", corpo);
    }

    /** Cancelamento de pedido RECUSADO pelo admin. */
    public static String cancelamentoRecusado(String nome, Long pedidoId, String tituloLivro,
            String comentarioAdmin) {
        String corpo = saudacao(nome) +
                paragrafo("Infelizmente, sua solicitação de cancelamento do pedido <strong>#" + pedidoId + "</strong> não pôde ser aprovada.") +
                tabelaFinanceira(
                        linhaSaldo("Livro", tituloLivro, COR_TEXTO)) +
                (comentarioAdmin != null && !comentarioAdmin.isBlank()
                        ? aviso("Motivo informado pela equipe: " + comentarioAdmin)
                        : "") +
                paragrafo("Seu pedido permanece ativo e será enviado normalmente. Se tiver dúvidas, entre em contato com nosso suporte.") +
                botao("Ver meus pedidos", "/clientes/homepage?aba=pedidos");
        return base("Cancelamento não aprovado — Bibliotroca", corpo);
    }

    /** Cancelamento de pedido PELO ADMIN (com estorno). */
    public static String cancelamentoAdmin(
            String nomeCliente,
            Long pedidoId,
            String tituloLivro,
            double preco,
            String dataCompra,
            String dataCancelamento,
            String motivoCategoria,
            String justificativa) {

        String corpo =
            paragrafo("Olá, <strong>" + escHtml(nomeCliente) + "</strong>.") +
            paragrafo("Informamos que o seu pedido foi <strong>cancelado</strong> pela nossa equipe administrativa. Os tokens referentes à compra foram <strong>estornados</strong> para o seu saldo.") +
            "<table style='width:100%;border-collapse:collapse;margin:1.5rem 0;font-size:.88rem;'>" +
            "<tr style='background:#f5f0e8;'><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;width:40%'>Pedido</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>#" + pedidoId + "</td></tr>" +
            "<tr><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Livro</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>" + escHtml(tituloLivro) + "</td></tr>" +
            "<tr style='background:#f5f0e8;'><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Valor estornado</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;color:#722f37;font-weight:700;'>T$ " + String.format("%.2f", preco) + "</td></tr>" +
            "<tr><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Data da compra</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>" + escHtml(dataCompra) + "</td></tr>" +
            "<tr style='background:#f5f0e8;'><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Data do cancelamento</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>" + escHtml(dataCancelamento) + "</td></tr>" +
            "<tr><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Motivo</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>" + escHtml(motivoCategoria) + "</td></tr>" +
            "<tr style='background:#f5f0e8;'><td style='padding:.6rem 1rem;font-weight:700;border:1px solid #e0d9d0;'>Detalhes</td><td style='padding:.6rem 1rem;border:1px solid #e0d9d0;'>" + escHtml(justificativa) + "</td></tr>" +
            "</table>" +
            paragrafo("Pedimos sinceras desculpas pelo transtorno causado. Os tokens já foram creditados em sua conta e estão disponíveis para novas compras.") +
            paragrafo("Se tiver dúvidas, entre em contato com nossa equipe pelo e-mail <a href='mailto:bibliotroca.noreply@gmail.com' style='color:#722f37;'>bibliotroca.noreply@gmail.com</a>.") +
            paragrafo("Atenciosamente,<br><strong>Equipe Bibliotroca</strong>");

        return base("Pedido cancelado — Bibliotroca", corpo);
    }

    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Aviso de pontos XP próximos ao vencimento. */
    public static String xpExpirando(String nome, long xpTotal, String dataExpiracao) {
        String corpo = saudacao(nome) +
                paragrafo("Seus pontos XP estão prestes a expirar. Mantenha-os ativos!") +
                tabelaFinanceira(
                        linhaSaldo("Pontos XP acumulados", String.valueOf(xpTotal), COR_PRIMARIA),
                        linhaSaldo("Data de expiração", dataExpiracao, COR_ALERTA))
                +
                "<h3 style='margin:20px 0 8px;font-size:15px;color:" + COR_MUTED + ";'>Como renovar seu prazo</h3>" +
                "<ul style='margin:0 0 16px;padding-left:20px;font-size:14px;line-height:1.8;color:" + COR_TEXTO + ";'>"
                +
                "<li>Realizar uma compra na vitrine</li>" +
                "<li>Avaliar um livro</li>" +
                "<li>Enviar um livro para aprovação</li>" +
                "</ul>" +
                paragrafo("Qualquer uma dessas ações renova automaticamente o prazo dos seus pontos.");
        return base("Seus pontos XP vão expirar em breve! — Bibliotroca", corpo);
    }
}
