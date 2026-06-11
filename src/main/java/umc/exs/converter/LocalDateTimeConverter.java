package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converte LocalDateTime para String no formato yyyy-MM-dd HH:mm:ss ao salvar no banco e faz o caminho inverso ao ler.
 * Garante compatibilidade com bancos que armazenam datas como texto, como o SQLite em ambiente local.
 */
@Converter(autoApply = false)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Formata um LocalDateTime para String no formato yyyy-MM-dd HH:mm:ss antes de persistir no banco.
     * Retorna null caso o atributo seja nulo.
     */
    @Override
    public String convertToDatabaseColumn(LocalDateTime attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.format(FORMATTER);
    }

    /**
     * Converte a String lida do banco para LocalDateTime usando o formato yyyy-MM-dd HH:mm:ss.
     * Lança RuntimeException caso o valor não esteja no formato esperado.
     */
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