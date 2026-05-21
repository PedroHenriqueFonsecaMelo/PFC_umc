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

@Converter
@Component
public class CpfConverter implements AttributeConverter<String, String> {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_LENGTH = 128;

    private final SecureRandom random = new SecureRandom();

    @Value("${jwt.secret:changeitchangeitchangeitchangeit}")
    private String secretKey;

    private SecretKeySpec getKey() {
        byte[] keyBytes = new byte[32];

        byte[] original = secretKey.getBytes(StandardCharsets.UTF_8);

        System.arraycopy(original, 0, keyBytes, 0,
                Math.min(original.length, 32));

        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String cpf) {
        if (cpf == null) return null;

        try {
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(),
                    new GCMParameterSpec(TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(
                    cpf.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar CPF", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || !dbData.contains(":")) {
            return dbData;
        }

        try {
            String[] parts = dbData.split(":");

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, getKey(),
                    new GCMParameterSpec(TAG_LENGTH, iv));

            return new String(
                    cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Erro ao descriptografar CPF",
                    e
            );
        }
    }
}