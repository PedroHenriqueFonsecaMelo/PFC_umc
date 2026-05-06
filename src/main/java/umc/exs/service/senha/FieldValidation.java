package umc.exs.service.senha;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

public final class FieldValidation {

    private FieldValidation() {
    }

    public static boolean validarCampos(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            // Ignora ID e campos Boolean (ex: termsAccepted, privacyAccepted)
            if (field.getName().equalsIgnoreCase("id"))
                continue;
            if (field.getType() == Boolean.class || field.getType() == boolean.class)
                continue;

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
        if (input == null || input.trim().isEmpty())
            return true;

        Pattern sqlPattern = Pattern.compile(
                "('|;|-{2})|(/\\*|\\*/)|(drop|select|insert|delete|update|alter|create|exec|union|and|or)\\s",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern xssPattern = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        return !sqlPattern.matcher(input).find() && !xssPattern.matcher(input).find();
    }

    public static String sanitize(String input) {
        if (input == null)
            return null;
        String trimmed = input.trim();
        if (trimmed.length() > 255)
            trimmed = trimmed.substring(0, 255);

        return trimmed.replaceAll("[^\\p{Alnum}\\s@._:\\/\\-çÇáéíóúÁÉÍÓÚãõÃÕüÜ]", "");
    }

    public static String sanitizeEmail(String email) {
        if (email == null)
            return null;

        String sanitizedEmail = email.replaceAll("[^A-Za-z0-9@._]", "").trim();

        if (sanitizedEmail.matches("^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$")) {
            return sanitizedEmail;
        }

        throw new IllegalArgumentException("Invalid email format after sanitization");
    }

    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;

        String emailRegex = "^[A-Za-z0-9._]+@[A-Za-z0-9.]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    public static boolean isValidCPF(String cpf) {
        if (cpf == null)
            return false;

        cpf = cpf.replaceAll("[^0-9]", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
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

    private static int calculateDV(String cpf, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < weightStart - 1; i++) {
            sum += (cpf.charAt(i) - '0') * weight--;
        }

        int remainder = sum % 11;
        return (remainder < 2) ? 0 : (11 - remainder);
    }

    public static boolean isValidCardExpiry(YearMonth expiryDate) {
        if (expiryDate == null)
            return false;
        return expiryDate.compareTo(YearMonth.now()) >= 0;
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 8)
            return false;
        String passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,20}$";
        return Pattern.compile(passwordRegex).matcher(password).matches();
    }

    public static boolean isValidCEP(String cep) {
        if (cep == null)
            return false;
        String cepRegex = "^\\d{5}-?\\d{3}$";
        return Pattern.compile(cepRegex).matcher(cep).matches();
    }

    public static boolean isValidGenero(String gen) {
        if (gen == null)
            return false;

        String normalizedGen = gen.trim().toUpperCase();

        return normalizedGen.equals("M") ||
                normalizedGen.equals("F") ||
                normalizedGen.equals("OUTRO");
    }

    public static LocalDate isValidBirthDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String cleanDateStr = dateStr.replaceAll("[^0-9]", "");

        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        DateTimeFormatter cleanFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        LocalDate date = null;

        if (cleanDateStr.length() == 8) {
            try {
                date = LocalDate.parse(cleanDateStr, cleanFormatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        if (date == null) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    date = LocalDate.parse(dateStr, formatter);
                    break;
                } catch (DateTimeParseException ignored) {
                }
            }
        }

        if (date != null && date.isBefore(LocalDate.now())) {
            if (isOver18(date)) {
                return date;
            }
        }

        return null;
    }

    public static boolean isOver18(LocalDate birthDate) {
        if (birthDate == null)
            return false;
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        return !birthDate.isAfter(eighteenYearsAgo);
    }

    /**
     * Mascara CPF para exibição segura no frontend.
     * Exemplo: "12345678901" → "***.***. 789-01"
     */
    public static String mascararCpf(String cpf) {
        if (cpf == null)
            return null;
        String digits = cpf.replaceAll("[^0-9]", "");
        if (digits.length() != 11)
            return "***.***.***-**";
        return String.format("***.***.%s-%s",
                digits.substring(6, 9),
                digits.substring(9));
    }
}
