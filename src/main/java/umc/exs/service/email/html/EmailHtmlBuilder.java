package umc.exs.service.email.html;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class EmailHtmlBuilder {

        private EmailHtmlBuilder() {
        }

        // =========================
        // COMPRA SIMPLES
        // =========================
        public static String compraSucesso(String nome, String livro, double preco) {

                nome = EmailSanitizer.esc(nome);
                livro = EmailSanitizer.esc(livro);

                String conteudo = EmailComponents.h2("Olá, " + nome)
                                + EmailComponents.p("Você comprou: " + livro)
                                + EmailComponents.caixa(
                                                EmailComponents.p("Valor: R$ " + preco));

                return EmailLayout.wrap("Compra confirmada", conteudo);
        }

        // =========================
        // COMPRA COMPLETA (USADA NO SISTEMA)
        // =========================
        public static String compraSucesso(String nome, String livro, Double preco, Double saldo, String data) {

                nome = EmailSanitizer.esc(nome);
                livro = EmailSanitizer.esc(livro);
                data = EmailSanitizer.esc(data);

                String conteudo = EmailComponents.h2("Compra realizada")
                                + EmailComponents.p("Livro: " + livro)
                                + EmailComponents.p("Valor: R$ " + preco)
                                + EmailComponents.p("Saldo restante: R$ " + saldo)
                                + EmailComponents.p("Data: " + data);

                return EmailLayout.wrap("Compra confirmada", conteudo);
        }

        // =========================
        // CARRINHO
        // =========================
        public static String carrinhoConfirmado(String nome, List<String[]> itens, double total, Double saldo,
                        String data) {

                nome = EmailSanitizer.esc(nome);
                data = EmailSanitizer.esc(data);

                StringBuilder lista = new StringBuilder();

                for (String[] item : itens) {
                        lista.append(EmailComponents.p(item[0] + " - R$ " + item[1]));
                }

                String conteudo = EmailComponents.h2("Olá, " + nome)
                                + EmailComponents.p("Seu carrinho foi processado:")
                                + lista
                                + EmailComponents.divider()
                                + EmailComponents.p("Total: R$ " + total)
                                + EmailComponents.p("Saldo restante: R$ " + saldo)
                                + EmailComponents.p("Data: " + data);

                return EmailLayout.wrap("Carrinho confirmado", conteudo);
        }

        // =========================
        // SALDO
        // =========================
        public static String atualizacaoSaldo(String nome, double valor, Double saldoAnterior, Double saldoAtual,
                        String operacao, boolean credito, LocalDateTime data) {

                nome = EmailSanitizer.esc(nome);
                operacao = EmailSanitizer.esc(operacao);

                String dataFormatada = data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

                String conteudo = EmailComponents.h2("Atualização de saldo")
                                + EmailComponents.p("Operação: " + operacao)
                                + EmailComponents.p("Valor: R$ " + valor)
                                + EmailComponents.p("Saldo anterior: R$ " + saldoAnterior)
                                + EmailComponents.p("Saldo atual: R$ " + saldoAtual)
                                + EmailComponents.p("Data: " + dataFormatada);

                return EmailLayout.wrap("Movimentação financeira", conteudo);
        }

        // =========================
        // PEDIDO
        // =========================
        public static String atualizacaoPedido(String nome, Long id, String status, String livro,
                        String mensagem, boolean pago, double valor, String data) {

                nome = EmailSanitizer.esc(nome);
                status = EmailSanitizer.esc(status);

                String conteudo = EmailComponents.h2("Pedido atualizado")
                                + EmailComponents.p("Pedido #" + id)
                                + EmailComponents.p("Livro: " + livro)
                                + EmailComponents.p("Status: " + status)
                                + EmailComponents.p("Mensagem: " + mensagem)
                                + EmailComponents.p("Valor: R$ " + valor)
                                + EmailComponents.p("Pago: " + (pago ? "Sim" : "Não"))
                                + EmailComponents.p("Data: " + data);

                return EmailLayout.wrap("Atualização de pedido", conteudo);
        }

        // =========================
        // CANCELAMENTO
        // =========================
        public static String cancelamentoAprovado(String nome, Long id, String livro,
                        double valor, Double saldo, String motivo) {

                String conteudo = EmailComponents.h2("Cancelamento aprovado")
                                + EmailComponents.p("Pedido #" + id)
                                + EmailComponents.p("Livro: " + livro)
                                + EmailComponents.p("Valor estornado: R$ " + valor)
                                + EmailComponents.p("Saldo atual: R$ " + saldo)
                                + EmailComponents.p("Motivo: " + motivo);

                return EmailLayout.wrap("Cancelamento aprovado", conteudo);
        }

        public static String cancelamentoRecusado(String nome, Long id, String livro, String motivo) {

                String conteudo = EmailComponents.h2("Cancelamento recusado")
                                + EmailComponents.p("Pedido #" + id)
                                + EmailComponents.p("Livro: " + livro)
                                + EmailComponents.p("Motivo: " + motivo);

                return EmailLayout.wrap("Cancelamento recusado", conteudo);
        }

        public static String cancelamentoAdmin(String nome, Long id, String livro,
                        double valor, String cliente, String email,
                        String motivo, String data) {

                String conteudo = EmailComponents.h2("Cancelamento solicitado")
                                + EmailComponents.p("Pedido #" + id)
                                + EmailComponents.p("Cliente: " + cliente)
                                + EmailComponents.p("Email: " + email)
                                + EmailComponents.p("Livro: " + livro)
                                + EmailComponents.p("Valor: R$ " + valor)
                                + EmailComponents.p("Motivo: " + motivo)
                                + EmailComponents.p("Data: " + data);

                return EmailLayout.wrap("Cancelamento (admin)", conteudo);
        }

        // =========================
        // LIVRO
        // =========================
        public static String livroAprovado(String nome, String livro, double valor) {
                return EmailLayout.wrap("Livro aprovado",
                                EmailComponents.h2("Seu livro foi aprovado")
                                                + EmailComponents.p(livro)
                                                + EmailComponents.p("Valor: R$ " + valor));
        }

        public static String livroRejeitado(String nome, String livro, String motivo) {
                return EmailLayout.wrap("Livro rejeitado",
                                EmailComponents.h2("Livro rejeitado")
                                                + EmailComponents.p(livro)
                                                + EmailComponents.p("Motivo: " + motivo));
        }

        // =========================
        // LISTA DESEJOS
        // =========================
        public static String listaDesejosDisponivel(String nome, String livro, String autor,
                        boolean disponivel, String link) {

                String conteudo = EmailComponents.h2("Livro disponível")
                                + EmailComponents.p(livro + " - " + autor)
                                + EmailComponents.p("Disponível: " + (disponivel ? "Sim" : "Não"))
                                + EmailComponents.button("Ver livro", link);

                return EmailLayout.wrap("Lista de desejos", conteudo);
        }

        // =========================
        // CUPOM
        // =========================
        public static String cupomExpirando(String nome, String codigo, Double valor, String data) {

                String conteudo = EmailComponents.h2("Cupom expirando")
                                + EmailComponents.p("Código: " + codigo)
                                + EmailComponents.p("Valor: R$ " + valor)
                                + EmailComponents.p("Expira em: " + data);

                return EmailLayout.wrap("Cupom", conteudo);
        }

        // =========================
        // SENHA
        // =========================
        public static String recuperacaoSenha(String nome, String link) {
                return EmailLayout.wrap("Recuperação de senha",
                                EmailComponents.h2("Redefinir senha")
                                                + EmailComponents.button("Redefinir", link));
        }

        // =========================
        // VERIFICAÇÃO
        // =========================
        public static String verificacaoEmail(String nome, String link) {
                return EmailLayout.wrap("Verificação de email",
                                EmailComponents.h2("Confirme seu email")
                                                + EmailComponents.button("Confirmar", link));
        }

        // =========================
        // ADMIN
        // =========================
        public static String comunicadoAdmin(String nome, String mensagem) {
                return EmailLayout.wrap("Comunicado",
                                EmailComponents.h2("Mensagem administrativa")
                                                + EmailComponents.p(mensagem));
        }
}