package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Criptografa e descriptografa o CPF automaticamente antes de salvar e ao ler do banco de dados.
 * Utiliza AES-256-GCM para garantir confidencialidade dos dados pessoais em conformidade com a LGPD.
 */
@Converter
public class CpfConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();


    private String secretKey = "1bab54b78d14792a3426e3543f27a96a";

    /**
     * Converte a chave secreta configurada em um SecretKeySpec de 32 bytes para o algoritmo AES-256.
     * Preenche com zeros caso a chave original seja menor que 32 bytes.
     */
    private SecretKeySpec getKey() {
        byte[] keyBytes = new byte[32];

        byte[] original = secretKey.getBytes(StandardCharsets.UTF_8);

        System.arraycopy(original, 0, keyBytes, 0,
                Math.min(original.length, 32));

        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * Criptografa o CPF com AES/GCM usando um IV aleatório gerado a cada chamada.
     * O valor persistido no banco tem o formato Base64(IV):Base64(dadosCriptografados), garantindo unicidade. // LGPD
     */
    @Override
    public String convertToDatabaseColumn(String cpf) {
        if (cpf == null)
            return null;

        try {
            // Gera um IV aleatório de 12 bytes para garantir que cada criptografia seja única // LGPD
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(),
                    new GCMParameterSpec(TAG_LENGTH, iv));

            // Criptografa o CPF e codifica IV e resultado em Base64 separados por ":" // LGPD
            byte[] encrypted = cipher.doFinal(
                    cpf.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar CPF", e);
        }
    }

    /**
     * Descriptografa o CPF ao ler do banco, separando IV e dados criptografados pelo delimitador ":".
     * Retorna o valor original sem modificação caso ele não esteja no formato criptografado. // LGPD
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || !dbData.contains(":")) {
            return dbData;
        }

        try {
            // Separa o IV dos dados criptografados usando o delimitador ":" // LGPD
            String[] parts = dbData.split(":");

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, getKey(),
                    new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erro ao descriptografar CPF",
                    e);
        }
    }
}
