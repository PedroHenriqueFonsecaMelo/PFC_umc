package umc.exs.service.email.html;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmailHtmlBuilder {

    private EmailHtmlBuilder() {
    }

    private static final String OLA = "Olá, ";
    private static final String LIVRO = "Livro: ";
    private static final String VALOR = "Valor: R$ ";
    private static final String DATA = "Data: ";
    private static final String PEDIDO = "Pedido #";
    private static final String MOTIVO = "Motivo: ";

    public static String compraSucesso(String nome, String livro, Double preco, Double saldo, String data) {

        nome = EmailSanitizer.esc(nome);
        livro = EmailSanitizer.esc(livro);
        data = EmailSanitizer.esc(data);

        String conteudo =
                EmailComponents.h2(OLA + nome + "!")
                        + EmailComponents.p("Sua compra foi <strong style='color:"
                        + EmailLayout.COR_SUCESSO
                        + ";'>confirmada com sucesso</strong>.")
                        + EmailComponents.caixa(
                        EmailComponents.p("<strong>" + LIVRO + "</strong> " + livro)
                                + EmailComponents.p("<strong>" + VALOR + "</strong> " + preco)
                                + EmailComponents.p("<strong>Saldo restante: R$ </strong>" + saldo)
                                + EmailComponents.p("<strong>" + DATA + "</strong> " + data))
                        + EmailComponents.p("Obrigado por utilizar a plataforma.");

        return EmailLayout.wrap("Compra realizada com sucesso", conteudo);
    }

    public static String carrinhoConfirmado(String nome, List<String[]> itens, double total, Double saldo,
                                            String data) {

        nome = EmailSanitizer.esc(nome);
        data = EmailSanitizer.esc(data);

        StringBuilder lista = new StringBuilder();

        for (String[] item : itens) {
            lista.append(EmailComponents.p(item[0] + " - R$ " + item[1]));
        }

        String conteudo =
                EmailComponents.h2(OLA + nome)
                        + EmailComponents.p("Seu carrinho foi processado:")
                        + lista
                        + EmailComponents.divider()
                        + EmailComponents.p("Total: R$ " + total)
                        + EmailComponents.p("Saldo restante: R$ " + saldo)
                        + EmailComponents.p(DATA + data);

        return EmailLayout.wrap("Carrinho confirmado", conteudo);
    }

    public static String atualizacaoSaldo(String nome, double valor, Double saldoAnterior, Double saldoAtual,
                                          String operacao, boolean credito, LocalDateTime data) {

        operacao = EmailSanitizer.esc(operacao);

        String dataFormatada = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        String conteudo =
                EmailComponents.h2("Atualização de saldo")
                        + EmailComponents.p("Operação: " + operacao)
                        + EmailComponents.p(VALOR + valor)
                        + EmailComponents.p("Saldo anterior: R$ " + saldoAnterior)
                        + EmailComponents.p("Saldo atual: R$ " + saldoAtual)
                        + EmailComponents.p(DATA + dataFormatada);

        return EmailLayout.wrap("Movimentação financeira", conteudo);
    }

    public static String atualizacaoPedido(String nome, Long id, String status, String livro,
                                          String mensagem, boolean pago, double valor, String data) {

        status = EmailSanitizer.esc(status);

        String conteudo =
                EmailComponents.h2("Pedido atualizado")
                        + EmailComponents.p(PEDIDO + id)
                        + EmailComponents.p(LIVRO + livro)
                        + EmailComponents.p("Status: " + status)
                        + EmailComponents.p("Mensagem: " + mensagem)
                        + EmailComponents.p(VALOR + valor)
                        + EmailComponents.p("Pago: " + (pago ? "Sim" : "Não"))
                        + EmailComponents.p(DATA + data);

        return EmailLayout.wrap("Atualização de pedido", conteudo);
    }

    public static String cancelamentoAprovado(String nome, Long id, String livro,
                                              double valor, Double saldo, String motivo) {

        String conteudo =
                EmailComponents.h2("Cancelamento aprovado")
                        + EmailComponents.p(PEDIDO + id)
                        + EmailComponents.p(LIVRO + livro)
                        + EmailComponents.p("Valor estornado: R$ " + valor)
                        + EmailComponents.p("Saldo atual: R$ " + saldo)
                        + EmailComponents.p(MOTIVO + motivo);

        return EmailLayout.wrap("Cancelamento aprovado", conteudo);
    }

    public static String cancelamentoRecusado(String nome, Long id, String livro, String motivo) {

        String conteudo =
                EmailComponents.h2("Cancelamento recusado")
                        + EmailComponents.p(PEDIDO + id)
                        + EmailComponents.p(LIVRO + livro)
                        + EmailComponents.p(MOTIVO + motivo);

        return EmailLayout.wrap("Cancelamento recusado", conteudo);
    }

    public static String cancelamentoAdmin(String nome, Long id, String livro,
                                           double valor, String cliente, String email,
                                           String motivo, String data) {

        String conteudo =
                EmailComponents.h2("Cancelamento solicitado")
                        + EmailComponents.p(PEDIDO + id)
                        + EmailComponents.p("Cliente: " + cliente)
                        + EmailComponents.p("Email: " + email)
                        + EmailComponents.p(LIVRO + livro)
                        + EmailComponents.p(VALOR + valor)
                        + EmailComponents.p(MOTIVO + motivo)
                        + EmailComponents.p(DATA + data);

        return EmailLayout.wrap("Cancelamento (admin)", conteudo);
    }

    public static String livroAprovado(String nome, String livro, double valor) {

        EmailSanitizer.esc(nome);
        livro = EmailSanitizer.esc(livro);

        String conteudo =
                EmailComponents.h2("Livro aprovado!")
                        + EmailComponents.p("Seu livro foi aprovado e já está disponível na vitrine.")
                        + EmailComponents.caixa(
                        EmailComponents.p("<strong>" + LIVRO + "</strong>" + livro)
                                + EmailComponents.p("<strong>Recompensa: R$ </strong>" + valor));

        return EmailLayout.wrap("Livro aprovado", conteudo);
    }

    public static String livroRejeitado(String nome, String livro, String motivo) {

        livro = EmailSanitizer.esc(livro);
        motivo = EmailSanitizer.esc(motivo);

        String conteudo =
                EmailComponents.h2("Livro não aprovado")
                        + EmailComponents.caixa(
                        EmailComponents.p("<strong>" + LIVRO + "</strong>" + livro))
                        + EmailComponents.aviso("<strong>" + MOTIVO + "</strong>" + motivo)
                        + EmailComponents.p("Você pode enviar uma nova versão futuramente.");

        return EmailLayout.wrap("Livro rejeitado", conteudo);
    }

    public static String listaDesejosDisponivel(String nome, String livro, String autor,
                                                boolean disponivel, String link) {

        String conteudo =
                EmailComponents.h2("Livro disponível")
                        + EmailComponents.p(livro + " - " + autor)
                        + EmailComponents.p("Disponível: " + (disponivel ? "Sim" : "Não"))
                        + EmailComponents.button("Ver livro", link);

        return EmailLayout.wrap("Lista de desejos", conteudo);
    }

    public static String cupomExpirando(String nome, String codigo, Double valor, String data) {

        String conteudo =
                EmailComponents.h2("Cupom expirando")
                        + EmailComponents.p("Código: " + codigo)
                        + EmailComponents.p(VALOR + valor)
                        + EmailComponents.p("Expira em: " + data);

        return EmailLayout.wrap("Cupom", conteudo);
    }

    public static String recuperacaoSenha(String nome, String link) {

        nome = EmailSanitizer.esc(nome);

        String conteudo =
                EmailComponents.h2(OLA + nome + "!")
                        + EmailComponents.p("Recebemos uma solicitação para redefinir sua senha.")
                        + EmailComponents.button("Redefinir minha senha", link)
                        + EmailComponents.aviso("Este link expira em 30 minutos.")
                        + EmailComponents.p("Se você não solicitou esta alteração, ignore este e-mail.");

        return EmailLayout.wrap("Recuperação de senha", conteudo);
    }

    public static String verificacaoEmail(String nome, String link) {

        nome = EmailSanitizer.esc(nome);

        String conteudo =
                EmailComponents.h2(OLA + nome + "!")
                        + EmailComponents.p("Obrigado por criar sua conta.")
                        + EmailComponents.p("Confirme seu endereço de e-mail clicando no botão abaixo.")
                        + EmailComponents.button("Confirmar meu e-mail", link)
                        + EmailComponents.aviso("O link expira em 24 horas.");

        return EmailLayout.wrap("Confirmação de e-mail", conteudo);
    }

    public static String comunicadoAdmin(String nome, String mensagem) {

        nome = EmailSanitizer.esc(nome);

        StringBuilder html = new StringBuilder();

        html.append(EmailComponents.h2(OLA + nome + "!"));

        for (String linha : mensagem.split("\\n")) {

            String l = EmailSanitizer.esc(linha);

            if (!l.isBlank()) {
                html.append(EmailComponents.p(l));
            }
        }

        html.append(EmailComponents.divider());
        html.append(EmailComponents.p("Mensagem enviada pela administração."));

        return EmailLayout.wrap("Comunicado", html.toString());
    }
}