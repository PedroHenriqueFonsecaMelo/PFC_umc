package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Converter(autoApply = false)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter SPACE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter BRAZIL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank())
            return null;

        try {
            return LocalDateTime.parse(dbData, ISO);
        } catch (DateTimeParseException ex) {
            // fallback para formato SQL
        }

        try {
            return LocalDateTime.parse(dbData, SPACE);
        } catch (DateTimeParseException ex) {
            // fallback para formato brasileiro
        }

        try {
            return LocalDateTime.parse(dbData, BRAZIL);
        } catch (DateTimeParseException ex) {
            // fallback para substring segura
        }

        try {
            if (dbData.length() >= 19) {
                return LocalDateTime.parse(dbData.substring(0, 19), SPACE);
            }
        } catch (Exception ex) {
            // último fallback silencioso
        }

        try {
            return LocalDateTime.parse(dbData);
        } catch (DateTimeParseException ex) {
            // falha total de parsing
        }

        return null;
    }
}