package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter(autoApply = false)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.format(FORMATTER);
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dbData, FORMATTER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter LocalDateTime: " + dbData, e);
        }
    }
}