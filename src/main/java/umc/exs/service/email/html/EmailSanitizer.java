package umc.exs.service.email.html;

public final class EmailSanitizer {

    private EmailSanitizer() {
    }

    public static String esc(String s) {
        if (s == null)
            return "";

        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
