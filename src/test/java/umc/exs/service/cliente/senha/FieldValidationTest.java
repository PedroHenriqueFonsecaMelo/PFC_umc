package umc.exs.service.cliente.senha;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FieldValidationTest {

    // --- CLASSES DUMMIES PARA TESTAR VALIDAR_CAMPOS (REFLECTION) ---
    static class ObjetoValido {
        private String nome = "Teste";
        private String email = "teste@email.com";
        private int id = 123; 
        private boolean ativo = true;
    }

    static class ObjetoComStringVazia {
        private String nome = "   ";
    }

    static class ObjetoComCampoNulo {
        private String nome = null;
    }

    @Test
    void validarCampos_CenariosDiversos() {
        assertFalse(FieldValidation.validarCampos(null));

        assertFalse(FieldValidation.validarCampos(new ObjetoComStringVazia()));
        assertFalse(FieldValidation.validarCampos(new ObjetoComCampoNulo()));
    }

    // --- TESTES DE SEGURANÇA (isSafe) ---
    @Test
    void isSafe_CenariosGerais() {
        assertTrue(FieldValidation.isSafe(""));
        assertTrue(FieldValidation.isSafe("   "));
        assertTrue(FieldValidation.isSafe("Texto Comum Válido 123"));
        
        // Injeções SQL (Branches do Matcher)
        assertFalse(FieldValidation.isSafe("SELECT * FROM usuario;"));
        assertFalse(FieldValidation.isSafe("admin' --"));
        assertFalse(FieldValidation.isSafe("1' OR '1'='1"));
        
        // Injeções XSS (Branches do Matcher)
        assertFalse(FieldValidation.isSafe("<script>alert(1)</script>"));
        assertFalse(FieldValidation.isSafe("<img src=x onerror=prompt(1)>"));
    }

    // --- TESTES DE SANITIZAÇÃO (sanitize / sanitizeEmail) ---
    @Test
    void sanitize_CenariosDiversos() {
        assertNull(FieldValidation.sanitize(null));
        assertEquals("TextoLimpo", FieldValidation.sanitize("  TextoLimpo  "));
        
        // Remove caracteres especiais não mapeados
        assertEquals("Teste_123-A", FieldValidation.sanitize("Teste_123-A#$*"));
        
        // Estoura o limite de 255 caracteres para forçar a branch do substring
        String textoLongo = "a".repeat(300);
        String resultadoLongo = FieldValidation.sanitize(textoLongo);
        assertEquals(255, resultadoLongo.length());
    }

    @Test
    void sanitizeEmail_CenariosDiversos() {
        assertNull(FieldValidation.sanitizeEmail(null));
        assertEquals("contato@domain.com", FieldValidation.sanitizeEmail(" CONTATO@domain.com "));
        
        // Forçar o estouro da exceção IllegalArgumentException após a limpeza falhar
        assertThrows(IllegalArgumentException.class, () -> FieldValidation.sanitizeEmail("invalid-email#$"));
    }

    // --- VALIDACAO DE EMAIL ---
    @Test
    void isValidEmail_CenariosDiversos() {
        assertFalse(FieldValidation.isValidEmail(null));
        assertTrue(FieldValidation.isValidEmail("valido@email.com"));
        assertFalse(FieldValidation.isValidEmail("invalido@.com"));
        assertFalse(FieldValidation.isValidEmail("invalido@com"));
    }

    // --- VALIDACAO DE CPF e MASCARÁ ---
    @Test
    void isValidCPF_CenariosDiversos() {

        // Nulos e básicos
        assertFalse(FieldValidation.isValidCPF(null));
        assertFalse(FieldValidation.isValidCPF(""));
        assertFalse(FieldValidation.isValidCPF("   "));

        // Tamanho inválido
        assertFalse(FieldValidation.isValidCPF("123"));
        assertFalse(FieldValidation.isValidCPF("1234567890")); // 10 dígitos
        assertFalse(FieldValidation.isValidCPF("123456789012")); // 12 dígitos

        // Sequências inválidas
        assertFalse(FieldValidation.isValidCPF("00000000000"));
        assertFalse(FieldValidation.isValidCPF("11111111111"));
        assertFalse(FieldValidation.isValidCPF("99999999999"));

        // CPF válido (sem máscara)
        assertTrue(FieldValidation.isValidCPF("52998224725"));

        // CPF válido (com máscara)
        assertTrue(FieldValidation.isValidCPF("529.982.247-25"));

        // CPF inválido com DV errado
        assertFalse(FieldValidation.isValidCPF("52998224729"));

        // CPF com caracteres misturados (deve limpar e validar)
        assertTrue(FieldValidation.isValidCPF("529.982.247-25abc"));

        // CPF com caracteres inválidos que quebram formato
        assertFalse(FieldValidation.isValidCPF("abc"));
    }

    @Test
    void mascararCpf_CenariosDiversos() {
        assertNull(FieldValidation.mascararCpf(null));
        assertEquals("***.***.***-**", FieldValidation.mascararCpf("1234"));
        assertEquals("***.***.247-25", FieldValidation.mascararCpf("52998224725"));
    }

    // --- VALIDACAO DE SENHA ---
    @Test
    void isValidPassword_CenariosDiversos() {
        assertFalse(FieldValidation.isValidPassword(null));
        assertFalse(FieldValidation.isValidPassword("Short1!")); // Menos de 8 caracteres
        
        // Regex complexa (mapeia todas as restrições solicitadas no padrão)
        assertTrue(FieldValidation.isValidPassword("ValidP@ss123")); 
        
        assertFalse(FieldValidation.isValidPassword("nocapital123!")); // Sem maiúscula
        assertFalse(FieldValidation.isValidPassword("NOSMALL123!"));   // Sem minúscula
        assertFalse(FieldValidation.isValidPassword("NoNumber!"));     // Sem número
        assertFalse(FieldValidation.isValidPassword("NoSpecial123"));   // Sem caractere especial
    }

    // --- VALIDAÇÃO DE CARTÃO E CEP ---
    @Test
    void isValidCardExpiry_Cenarios() {
        assertFalse(FieldValidation.isValidCardExpiry(null));
        assertTrue(FieldValidation.isValidCardExpiry(YearMonth.now().plusMonths(1)));
        assertFalse(FieldValidation.isValidCardExpiry(YearMonth.now().minusMonths(1)));
    }

    @Test
    void isValidCEP_Cenarios() {
        assertFalse(FieldValidation.isValidCEP(null));
        assertTrue(FieldValidation.isValidCEP("12345-678"));
        assertTrue(FieldValidation.isValidCEP("12345678"));
        assertFalse(FieldValidation.isValidCEP("1234-567"));
    }

    // --- VALIDAÇÃO DE GÊNERO ---
    @ParameterizedTest
    @ValueSource(strings = {"m", "M", "  f  ", "F", "outro", "OUTRO"})
    void isValidGenero_CenariosValidos(String input) {
        assertTrue(FieldValidation.isValidGenero(input));
    }

    @Test
    void isValidGenero_CenariosInvalidos() {
        assertFalse(FieldValidation.isValidGenero(null));
        assertFalse(FieldValidation.isValidGenero("Masculino"));
        assertFalse(FieldValidation.isValidGenero("X"));
    }

    // --- VALIDAÇÃO DE MAIORIDADE (isOver18) ---
    @Test
    void isOver18_CenariosDiversos() {
        assertFalse(FieldValidation.isOver18(null));
        assertTrue(FieldValidation.isOver18(LocalDate.now().minusYears(19))); // 19 anos atrás (Ok)
        assertFalse(FieldValidation.isOver18(LocalDate.now().minusYears(17))); // Menor de idade
        assertFalse(FieldValidation.isOver18(LocalDate.now().plusDays(1))); // Data futura
    }
}