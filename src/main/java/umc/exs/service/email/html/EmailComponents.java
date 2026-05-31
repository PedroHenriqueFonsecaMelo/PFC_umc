package umc.exs.service.email.html;

public class EmailComponents {

    private EmailComponents() {
    }

    public static String h1(String texto) {
        return "<h1 style='margin:0 0 10px 0;'>" + texto + "</h1>";
    }

    public static String h2(String texto) {
        return "<h2 style='margin:0 0 10px 0;'>" + texto + "</h2>";
    }

    public static String p(String texto) {
        return "<p style='margin:0 0 10px 0;'>" + texto + "</p>";
    }

    public static String strong(String texto) {
        return "<strong>" + texto + "</strong>";
    }

    public static String divider() {
        return "<hr style='border:none; border-top:1px solid #ddd; margin:15px 0;'>";
    }

    public static String button(String texto, String link) {
        return "<a href='" + link + "' "
                + "style='display:inline-block; padding:10px 15px; background-color:#007bff; color:#fff; text-decoration:none; border-radius:4px;'>"
                + texto
                + "</a>";
    }

    public static String caixa(String conteudo) {
        return "<div style='padding:10px; border:1px solid #ddd; background-color:#fafafa;'>"
                + conteudo
                + "</div>";
    }
}