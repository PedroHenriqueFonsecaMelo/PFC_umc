package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class EmailMaskConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Quando salvar no banco, salva o e-mail normal (puro)
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Quando o JPA ler do banco, ele já entrega mascarado para a Entidade!
        if (dbData == null || !dbData.contains("@")) {
            return dbData;
        }

        String[] partes = dbData.split("@");
        String local = partes[0];
        String dominio = partes[1];

        if (local.length() <= 2) {
            return "**@" + dominio;
        }

        return local.substring(0, 2) + "*".repeat(local.length() - 2) + "@" + dominio;
    }
}