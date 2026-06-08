package umc.exs.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Converter
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        return attribute != null ? attribute.format(FMT) : null;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        try {
            return LocalDateTime.parse(dbData, FMT);
        } catch (Exception e) {
            // fallback para milissegundos epoch
            try {
                long millis = Long.parseLong(dbData.trim());
                return java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDateTime();
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
