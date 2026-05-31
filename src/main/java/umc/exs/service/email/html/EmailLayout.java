package umc.exs.service.email.html;

public class EmailLayout {

    private EmailLayout() {
    }

    public static String wrap(String titulo, String conteudo) {

        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<title>" + titulo + "</title>"
                + "</head>"
                + "<body style='margin:0; padding:0; font-family:Arial, sans-serif; background-color:#f4f4f4;'>"

                + "<table width='100%' cellpadding='0' cellspacing='0' style='background-color:#f4f4f4;'>"
                + "<tr>"
                + "<td align='center'>"

                + "<table width='600' cellpadding='20' cellspacing='0' style='background-color:#ffffff; margin-top:20px;'>"

                // HEADER
                + "<tr>"
                + "<td style='background-color:#222; color:#fff; text-align:center;'>"
                + "<h1 style='margin:0;'>" + titulo + "</h1>"
                + "</td>"
                + "</tr>"

                // BODY
                + "<tr>"
                + "<td style='color:#333; font-size:14px;'>"
                + conteudo
                + "</td>"
                + "</tr>"

                // FOOTER
                + "<tr>"
                + "<td style='background-color:#eee; text-align:center; font-size:12px; color:#777;'>"
                + "Sistema UMC"
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