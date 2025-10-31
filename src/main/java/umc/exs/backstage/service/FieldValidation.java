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
                if (value == null)
                    return false;
                if (value instanceof String && ((String) value).trim().isEmpty())
                    return false;
            } catch (IllegalAccessException e) {
                return false;
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
        return !pattern.matcher(input).find();
    }

    // simples sanitização: remove caracteres potencialmente perigosos e limita
    // comprimento
    public static String sanitize(String input) {
        if (input == null)
            return null;
        String trimmed = input.trim();
        if (trimmed.length() > 255)
            trimmed = trimmed.substring(0, 255);
        // permite letras, números, espaço, @ . - _ : /
        return trimmed.replaceAll("[^\\p{Alnum}\\s@._:\\/-]", "");
    }

    // valida e sanitiza email básico
    public static String sanitizeEmail(String email) {
        if (email == null)
            return null;
        String s = sanitize(email);
        return s.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$") ? s : null;
    }

}
