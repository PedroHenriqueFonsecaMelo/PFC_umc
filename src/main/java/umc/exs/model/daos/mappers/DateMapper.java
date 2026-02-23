package umc.exs.model.daos.mappers;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class DateMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");

    @Named("stringToYearMonth")
    public YearMonth stringToYearMonth(String validade) {
        if (validade == null || validade.isBlank()) return null;
        try {
            // Tenta MM/yy primeiro (padrão de cartões)
            return YearMonth.parse(validade, formatter);
        } catch (Exception e) {
            // Fallback para ISO yyyy-MM caso venha do banco/input type month
            return YearMonth.parse(validade);
        }
    }

    @Named("yearMonthToString")
    public String yearMonthToString(YearMonth validade) {
        if (validade == null) return null;
        return validade.format(formatter);
    }
}