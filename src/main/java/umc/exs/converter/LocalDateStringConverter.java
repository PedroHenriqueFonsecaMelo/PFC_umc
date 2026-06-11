package umc.exs.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Converte LocalDate para String no formato yyyy-MM-dd ao salvar no banco e faz o caminho inverso ao ler.
 * Garante compatibilidade com bancos de dados que armazenam datas como texto em vez de tipo nativo.
 */
@Converter(autoApply = false)
public class LocalDateStringConverter implements AttributeConverter<LocalDate, String> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Converte um LocalDate para String no formato yyyy-MM-dd antes de persistir no banco.
     * Retorna null caso o atributo seja nulo.
     */
    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.format(FORMATTER);
    }

    /**
     * Converte a String lida do banco para LocalDate usando o formato yyyy-MM-dd.
     * Lança RuntimeException caso o valor não esteja no formato esperado.
     */
    @Override
    public LocalDate convertToEntityAttribute(String dbData) {

        if (dbData == null || dbData.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dbData, FORMATTER);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter LocalDate: " + dbData, e);
        }
    }
}