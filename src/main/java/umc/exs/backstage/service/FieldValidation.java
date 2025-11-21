package umc.exs.backstage.service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class FieldValidation {

    private FieldValidation() {
    }

    /**
     * Verifica se todos os campos de um DTO são não nulos/não vazios.
     */
    public static boolean validarCampos(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            
            // Pula o campo 'id' (geralmente gerado automaticamente)
            if (field.getName().equalsIgnoreCase("id")) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value == null) {
                    return false;
                }
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return true;
    }

    // ==========================================================
    // ⚔️ SQL/XSS INJECTION GUARD
    // ==========================================================

    /**
     * Verifica se a string contém padrões comuns de ataque SQL Injection ou XSS.
     * Esta é a sua principal defesa na validação.
     */
    public static boolean isSafe(String input) {
        if (input == null || input.trim().isEmpty())
            return true; // Considera null/vazio seguro (sem comandos)

        // Padrão SQL Injection: comandos DDL/DML, OR/AND 1=1, etc.
        Pattern sqlPattern = Pattern.compile(
                "('|;|-{2})|(/\\*|\\*/)|(drop|select|insert|delete|update|alter|create|exec|union|and|or)\\s",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL); 
        
        // Padrão XSS: tags HTML básicas
        Pattern xssPattern = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        // Verifica a presença de padrões de ataque
        return !sqlPattern.matcher(input).find() && !xssPattern.matcher(input).find();
    }

    /** 
     * @param input
     * @return String
     */
    // SANITIZAÇÃO GERAL: Remove apenas caracteres perigosos, mas mantém a maioria
    // dos caracteres para nomes, endereços, etc.
    public static String sanitize(String input) {
        if (input == null)
            return null;
        String trimmed = input.trim();
        if (trimmed.length() > 255)
            trimmed = trimmed.substring(0, 255);

        // Remove: caracteres não alfanuméricos, exceto espaço e o set comum de pontuação para endereços
        // Esta é uma limpeza de caracteres ilegais/não esperados, não a principal defesa contra SQLi.
        // Incluído 'ç' para suporte a caracteres locais (pode ser expandido para \p{L})
        return trimmed.replaceAll("[^\\p{Alnum}\\s@._:\\/\\-çÇáéíóúÁÉÍÓÚãõÃÕüÜ]", "");
    }

    // ==========================================================
    // 📧 EMAIL
    // ==========================================================
    
    /**
     * Sanitiza o email removendo caracteres especiais (exceto @, . e _), 
     * e garante que o formato resultante seja válido (usando a regra restrita).
     */
    public static String sanitizeEmail(String email) {
        if (email == null)
            return null;

        // 1. SANITIZAÇÃO: Remove TUDO que não for letra, número, @, ponto (.) ou underline (_).
        // Bloqueia intencionalmente o '+' e o '%'.
        String sanitizedEmail = email.replaceAll("[^A-Za-z0-9@._]", "").trim();

        // 2. Verifica o formato final com a REGRA RESTRITA:
        if (sanitizedEmail.matches("^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$")) {
            return sanitizedEmail.toLowerCase(); // Padroniza
        }
        
        throw new IllegalArgumentException("Invalid email format after sanitization");
    }

    /**
     * Valida o formato do email com base na regra RESTRESTRITA.
     */
    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;
        
        String emailRegex = "^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    /** 
     * @param cpf
     * @return boolean
     */
    // ==========================================================
    // 💳 CARTÃO / CPF / SENHA / GÊNERO / CEP
    // ==========================================================
    
    public static boolean isValidCPF(String cpf) {
        if (cpf == null)
            return false;

        cpf = cpf.replaceAll("[^0-9]", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // --- CÁLCULO DOS DÍGITOS VERIFICADORES ---
            int calculatedDV1 = calculateDV(cpf, 10);
            if (calculatedDV1 != (cpf.charAt(9) - '0')) {
                return false;
            }

            int calculatedDV2 = calculateDV(cpf, 11);
            return calculatedDV2 == (cpf.charAt(10) - '0');

        } catch (Exception e) {
            return false;
        }
    }
    
    /** 
     * @param cpf
     * @param weightStart
     * @return int
     */
    private static int calculateDV(String cpf, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < weightStart - 1; i++) {
            sum += (cpf.charAt(i) - '0') * weight--;
        }

        int remainder = sum % 11;
        return (remainder < 2) ? 0 : (11 - remainder);
    }

    /** 
     * @param expiryDate
     * @return boolean
     */
    public static boolean isValidCardExpiry(YearMonth expiryDate) {
        if (expiryDate == null)
            return false;
        return expiryDate.compareTo(YearMonth.now()) >= 0;
    }

    /** 
     * @param password
     * @return boolean
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8)
            return false;
        // Requer: 8-20 caracteres, maiúscula, minúscula, número e símbolo (@#$%^&+=!)
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$";
        return Pattern.compile(passwordRegex).matcher(password).matches();
    }

    /** 
     * @param cep
     * @return boolean
     */
    public static boolean isValidCEP(String cep) {
        if (cep == null)
            return false;
        String cepRegex = "^\\d{5}-?\\d{3}$";
        return Pattern.compile(cepRegex).matcher(cep).matches();
    }

    /** 
     * @param gen
     * @return boolean
     */
    public static boolean isValidGenero(String gen) {
        if (gen == null)
            return false;

        String normalizedGen = gen.trim().toUpperCase();

        return normalizedGen.equals("M") ||
                normalizedGen.equals("F") ||
                normalizedGen.equals("OUTRO");
    }

    /** 
     * @param dateStr
     * @return LocalDate
     */
    // ==========================================================
    // 📅 DATAS
    // ==========================================================
    
    public static LocalDate isValidBirthDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String cleanDateStr = dateStr.replaceAll("[^0-9]", "");
        
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"), // Padrão HTML
                DateTimeFormatter.ofPattern("dd/MM/yyyy"), // Padrão BR
                DateTimeFormatter.ofPattern("MM/dd/yyyy"), // Padrão EUA
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        DateTimeFormatter cleanFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        LocalDate date = null;

        // 1. Tenta o formato limpo (Ex: 01011990)
        if (cleanDateStr.length() == 8) {
            try {
                date = LocalDate.parse(cleanDateStr, cleanFormatter);
            } catch (DateTimeParseException ignored) {}
        }

        // 2. Tenta os formatos com separadores
        if (date == null) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    date = LocalDate.parse(dateStr, formatter);
                    break;
                } catch (DateTimeParseException ignored) {}
            }
        }

        // 3. VALIDAÇÃO: Não é nulo, não é futura e é maior de 18
        if (date != null && date.isBefore(LocalDate.now())) {
            if (isOver18(date)) {
                return date;
            }
        }

        return null;
    }

    /** 
     * @param birthDate
     * @return boolean
     */
    public static boolean isOver18(LocalDate birthDate) {
        if (birthDate == null)
            return false;
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        return !birthDate.isAfter(eighteenYearsAgo);
    }
}