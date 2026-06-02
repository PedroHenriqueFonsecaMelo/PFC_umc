package umc.exs.service.email.html;

public class EmailLayout {

    private EmailLayout() {
    }

    public static final String COR_PRIMARIA = "#722F37";
    public static final String COR_OURO = "#c9a96e";
    public static final String COR_FUNDO = "#f9f6f0";
    public static final String COR_TEXTO = "#2c241b";
    public static final String COR_MUTED = "#7a6e65";
    public static final String COR_SUCESSO = "#2e7d32";
    public static final String COR_ALERTA = "#b45309";

    public static String wrap(String titulo, String conteudo) {

        return "<!DOCTYPE html>"
                + "<html lang='pt-BR'>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + titulo + "</title>"
                + "</head>"

                + "<body style='margin:0;padding:0;"
                + "background:" + COR_FUNDO + ";"
                + "font-family:Arial,Helvetica,sans-serif;"
                + "color:" + COR_TEXTO + ";'>"

                + "<table width='100%' cellpadding='0' cellspacing='0'"
                + " style='background:" + COR_FUNDO + ";'>"

                + "<tr>"
                + "<td align='center' style='padding:32px 16px;'>"

                + "<table width='600' cellpadding='0' cellspacing='0'"
                + " style='max-width:600px;width:100%;"
                + "background:#fff;"
                + "border-radius:8px;"
                + "overflow:hidden;"
                + "box-shadow:0 2px 8px rgba(0,0,0,.08);'>"

                // HEADER
                + "<tr>"
                + "<td style='background:" + COR_PRIMARIA + ";"
                + "padding:28px 40px;"
                + "text-align:center;'>"

                + "<h1 style='margin:0;"
                + "font-size:26px;"
                + "color:#fff;"
                + "letter-spacing:2px;"
                + "font-family:Georgia,serif;'>"

                + "Bibliotroca"

                + "</h1>"

                + "<p style='margin:4px 0 0;"
                + "font-size:13px;"
                + "color:" + COR_OURO + ";"
                + "letter-spacing:1px;'>"

                + "Sua biblioteca de trocas"

                + "</p>"
                + "</td>"
                + "</tr>"

                // BODY
                + "<tr>"
                + "<td style='padding:36px 40px;'>"
                + conteudo
                + "</td>"
                + "</tr>"

                // FOOTER
                + "<tr>"
                + "<td style='background:#f3ede6;"
                + "padding:20px 40px;"
                + "text-align:center;"
                + "border-top:1px solid #e8ddd4;'>"

                + "<p style='margin:0;"
                + "font-size:12px;"
                + "color:" + COR_MUTED + ";'>"

                + "Este e-mail foi enviado automaticamente. Não responda."

                + "</p>"

                + "<p style='margin:6px 0 0;"
                + "font-size:13px;"
                + "color:" + COR_PRIMARIA + ";"
                + "font-weight:bold;'>"

                + "Equipe Bibliotroca"

                + "</p>"
                + "</td>"
                + "</tr>"

                + "</table>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}