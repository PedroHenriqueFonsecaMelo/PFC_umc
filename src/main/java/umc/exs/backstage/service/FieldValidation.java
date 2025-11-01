package umc.exs.backstage.service;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public final class FieldValidation {

    private FieldValidation() {
    }

    public static boolean validarCampos(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value == null) {
                    return false; // Campo nulo
                }
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    return false; // Campo de string vazio ou só com espaços
                }
            } catch (IllegalAccessException e) {
                return false; // Erro ao acessar o campo
            }
        }
        return true;
    }

    public static boolean isSafe(String input) {
        if (input == null)
            return true;

        // Proíbe comandos SQL comuns
        Pattern pattern = Pattern.compile("('|;|-{2})|(drop|select|insert|delete|update|alter|create|exec|union)\\s",
                Pattern.CASE_INSENSITIVE);
        // Proíbe conteúdo potencialmente perigoso relacionado a XSS
        Pattern xssPattern = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE);

        return !pattern.matcher(input).find() && !xssPattern.matcher(input).find();
    }

    // simples sanitização: remove caracteres potencialmente perigosos e limita
    // comprimento
    public static String sanitize(String input) {
        if (input == null)
            return null;
        String trimmed = input.trim();
        if (trimmed.length() > 255)
            trimmed = trimmed.substring(0, 255);

        // Limpeza dos caracteres permitidos
        return trimmed.replaceAll("[^\\p{Alnum}\\s@._:\\/-]", "");
    }

    // valida e sanitiza email básico
    public static String sanitizeEmail(String email) {
        if (email == null)
            return null;
        String sanitizedEmail = sanitize(email);
        if (sanitizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return sanitizedEmail;
        }
        throw new IllegalArgumentException("Invalid email format");
    }

    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

}
