package umc.exs.service;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

public class FieldValidation {
  
    public static boolean validarCampos(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value == null) return false;
                if (value instanceof String && ((String) value).trim().isEmpty()) return false;
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSafe(String input) {
        if (input == null) return true;
        // Proíbe comandos SQL comuns
        Pattern pattern = Pattern.compile("('|;|-{2})|(drop|select|insert|delete|update|alter|create|exec|union)\\s", Pattern.CASE_INSENSITIVE);        
        return !pattern.matcher(input).find();
    }
}

