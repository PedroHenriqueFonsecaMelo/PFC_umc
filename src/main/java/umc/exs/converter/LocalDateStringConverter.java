package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.LocalDate;

@Converter(autoApply = false)
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank())
            return null;
        // Tenta formato padrão ISO (yyyy-MM-dd)
        try {
            return LocalDate.parse(dbData);
        } catch (Exception e1) {
            // Tenta formato brasileiro (dd/MM/yyyy)
            try {
                return LocalDate.parse(dbData,
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e2) {
                // Tenta formato com hora (yyyy-MM-dd HH:mm:ss)
                try {
                    return LocalDate.parse(dbData.substring(0, 10));
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }
}
