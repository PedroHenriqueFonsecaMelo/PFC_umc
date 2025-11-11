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

    public static boolean validarCampos(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {

            // --- CORREÇÃO: PULA CAMPOS QUE DEVEM SER NULL ---
            // 1. Pula o campo 'id' (geralmente gerado automaticamente)
            if (field.getName().equalsIgnoreCase("id")) {
                continue;
            }
            // 2. Você pode adicionar outros campos que podem ser null aqui, como 'usuario'
            // if (field.getName().equalsIgnoreCase("usuario")) {
            // continue;
            // }
            // ------------------------------------------------

            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value == null) {
                    return false; // Falha se for um campo de DTO obrigatório
                }
                if (value instanceof String && ((String) value).trim().isEmpty()) {
                    return false; // Falha se for string vazia
                }
            } catch (IllegalAccessException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSafe(String input) {
        if (input == null)
            return true;

        Pattern pattern = Pattern.compile("('|;|-{2})|(drop|select|insert|delete|update|alter|create|exec|union)\\s",
                Pattern.CASE_INSENSITIVE);
        Pattern xssPattern = Pattern.compile("<.*?>", Pattern.CASE_INSENSITIVE);

        return !pattern.matcher(input).find() && !xssPattern.matcher(input).find();
    }

    // SANITIZAÇÃO GERAL: Permite alfanuméricos, espaços e um conjunto razoável de
    // símbolos (Restaurado)
    // Usado para nomes, endereços, etc.
    public static String sanitize(String input) {
        if (input == null)
            return null;
        String trimmed = input.trim();
        if (trimmed.length() > 255)
            trimmed = trimmed.substring(0, 255);

        // Permite: alfanuméricos, espaço, @ . _ : / -
        return trimmed.replaceAll("[^\\p{Alnum}\\s@._:\\/-]", "");
    }

    // VALIDA E SANITIZA EMAIL BÁSICO (APENAS PONTO, UNDERLINE E @ PERMITIDOS ALÉM
    // DE LETRAS/NÚMEROS)
    public static String sanitizeEmail(String email) {
        if (email == null)
            return null;

        // 1. SANITIZAÇÃO ESPECÍFICA: Remove caracteres que não sejam letras (A-Za-z),
        // números (0-9), @, ponto (.) ou underline (_).
        String sanitizedEmail = email.replaceAll("[^A-Za-z0-9@._]", "").trim();

        // 2. Verifica o formato final após a sanitização
        if (sanitizedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return sanitizedEmail.toLowerCase(); // Retorna em minúsculo para padronização
        }
        // Se a sanitização resultar em um formato inválido, lança exceção.
        throw new IllegalArgumentException("Invalid email format after sanitization");
    }

    public static boolean isValidEmail(String email) {
        if (email == null)
            return false;
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    // FieldValidation.java

    public static boolean isValidCPF(String cpf) {
        if (cpf == null)
            return false;

        // 1. LIMPEZA: Remove qualquer caractere que não seja um dígito.
        cpf = cpf.replaceAll("[^0-9]", "");

        // 2. CHECAGEM DE TAMANHO
        if (cpf.length() != 11) {
            return false;
        }

        // 3. REGRA ANTI-FRAUDE: Bloqueia sequências de dígitos iguais (ex: 11111111111)
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // --- 4. CÁLCULO DO PRIMEIRO DÍGITO VERIFICADOR (DV1) ---
            int sum1 = 0;
            int weight = 10;
            // Pondera os 9 primeiros dígitos
            for (int i = 0; i < 9; i++) {
                // (cpf.charAt(i) - '0') converte o char para seu valor inteiro
                sum1 += (cpf.charAt(i) - '0') * weight--;
            }

            int remainder1 = sum1 % 11;
            // Se o resto for 0 ou 1, o dígito verificador é 0. Caso contrário, é 11 -
            // resto.
            int calculatedDV1 = (remainder1 < 2) ? 0 : (11 - remainder1);

            // Verifica se o DV1 calculado corresponde ao 10º dígito do CPF
            if (calculatedDV1 != (cpf.charAt(9) - '0')) {
                return false;
            }

            // --- 5. CÁLCULO DO SEGUNDO DÍGITO VERIFICADOR (DV2) ---
            int sum2 = 0;
            weight = 11;
            // Pondera os 10 primeiros dígitos (incluindo o DV1)
            for (int i = 0; i < 10; i++) {
                sum2 += (cpf.charAt(i) - '0') * weight--;
            }

            int remainder2 = sum2 % 11;
            int calculatedDV2 = (remainder2 < 2) ? 0 : (11 - remainder2);

            // Retorna verdadeiro se o DV2 calculado corresponde ao último dígito do CPF
            return calculatedDV2 == (cpf.charAt(10) - '0');

        } catch (Exception e) {
            // Captura qualquer erro de conversão de caractere, garantindo 'false' em caso
            // de falha.
            return false;
        }
    }

    public static boolean isValidCardExpiry(YearMonth expiryDate) {
        if (expiryDate == null)
            return false;
        return expiryDate.compareTo(YearMonth.now()) >= 0;
    }

    public static LocalDate isValidBirthDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        // 1. Limpeza para tentar o formato sem separadores
        String cleanDateStr = dateStr.replaceAll("[^0-9]", "");

        // 2. Definindo os Formatadores que serão tentados
        DateTimeFormatter[] formatters = new DateTimeFormatter[] {
                // Padrão de formulário HTML (yyyy-MM-dd)
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                // Padrão Brasileiro (dd/MM/yyyy)
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                // Padrão Americano (MM/dd/yyyy)
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                // Padrão com traço (dd-MM-yyyy)
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        // Formatador para data limpa (ddMMyyyy)
        DateTimeFormatter cleanFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");

        LocalDate date = null;

        // A. Tenta o formato limpo primeiro (Ex: 01011990)
        if (cleanDateStr.length() == 8) {
            try {
                date = LocalDate.parse(cleanDateStr, cleanFormatter);
            } catch (DateTimeParseException ignored) {
                // Ignora e tenta os outros formatos
            }
        }

        // B. Tenta os formatos com separadores (se ainda não achou)
        if (date == null) {
            for (DateTimeFormatter formatter : formatters) {
                try {
                    date = LocalDate.parse(dateStr, formatter);
                    break; // Encontrou e saiu do loop
                } catch (DateTimeParseException ignored) {
                    // Tenta o próximo
                }
            }
        }

        // 3. VALIDAÇÃO: Verifica se o parse funcionou, se a data não é futura e se a
        // pessoa é maior de 18.
        if (date != null && date.isBefore(LocalDate.now())) {
            // Chamada ao seu método auxiliar.
            // Se a validação de maioridade estiver fora deste método, você deve chamá-la
            // aqui:
            // Exemplo: if (isOver18(date)) return date;

            // --- CHAME O MÉTODO isOver18 AQUI ---
            // (Assumindo que o isOver18 está na mesma classe e funciona corretamente)
            LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
            if (!date.isAfter(eighteenYearsAgo)) {
                return date; // Data válida e maior de 18 anos
            }
        }

        // Se falhou em qualquer etapa
        return null;
    }

    public static boolean isOver18(LocalDate birthDate) {
        if (birthDate == null)
            return false;
        LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
        return !birthDate.isAfter(eighteenYearsAgo);
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

    // NOVO MÉTODO: Validação do Gênero (M, F, OUTRO)
    public static boolean isValidGenero(String gen) {
        if (gen == null)
            return false;

        // Padroniza para maiúsculas e verifica se corresponde a um dos valores
        // esperados.
        String normalizedGen = gen.trim().toUpperCase();

        return normalizedGen.equals("M") ||
                normalizedGen.equals("F") ||
                normalizedGen.equals("OUTRO");
    }
}