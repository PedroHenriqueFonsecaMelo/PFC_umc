package umc.exs.service.email.html;

public class EmailComponents {

    private EmailComponents() {
    }

    public static String h1(String texto) {
        return "<h1 style='margin:0 0 16px;"
                + "color:" + EmailLayout.COR_PRIMARIA + ";"
                + "font-family:Georgia,serif;'>"
                + texto
                + "</h1>";
    }

    public static String h2(String texto) {
        return "<h2 style='margin:0 0 16px;"
                + "font-size:18px;"
                + "color:" + EmailLayout.COR_PRIMARIA + ";"
                + "font-family:Georgia,serif;'>"
                + texto
                + "</h2>";
    }

    public static String p(String texto) {
        return "<p style='margin:0 0 14px;"
                + "font-size:15px;"
                + "line-height:1.6;"
                + "color:" + EmailLayout.COR_TEXTO + ";'>"
                + texto
                + "</p>";
    }

    public static String strong(String texto) {
        return "<strong>" + texto + "</strong>";
    }

    public static String divider() {
        return "<hr style='border:none;"
                + "border-top:1px solid #e8ddd4;"
                + "margin:20px 0;' />";
    }

    public static String button(String texto, String link) {

        return "<div style='text-align:center;margin:24px 0;'>"
                + "<a href='" + link + "'"
                + " style='display:inline-block;"
                + "padding:13px 32px;"
                + "background:" + EmailLayout.COR_PRIMARIA + ";"
                + "color:#fff;"
                + "text-decoration:none;"
                + "border-radius:6px;"
                + "font-size:15px;"
                + "font-weight:bold;'>"
                + texto
                + "</a>"
                + "</div>";
    }

    public static String caixa(String conteudo) {

        return "<table width='100%' cellpadding='0' cellspacing='0'"
                + " style='margin:16px 0;"
                + "border:1px solid #e8ddd4;"
                + "border-radius:6px;"
                + "background:#faf8f4;'>"
                + "<tr>"
                + "<td style='padding:14px 18px;'>"
                + conteudo
                + "</td>"
                + "</tr>"
                + "</table>";
    }

    public static String aviso(String texto) {

        return "<div style='background:#fff8f0;"
                + "border-left:4px solid " + EmailLayout.COR_ALERTA + ";"
                + "padding:12px 16px;"
                + "margin:16px 0;"
                + "border-radius:0 4px 4px 0;'>"

                + "<p style='margin:0;"
                + "font-size:13px;"
                + "color:" + EmailLayout.COR_ALERTA + ";'>"

                + texto

                + "</p>"
                + "</div>";
    }
}