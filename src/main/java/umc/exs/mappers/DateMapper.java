package umc.exs.mappers;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class DateMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");

    @Named("yearMonthToString") // Usado no paraEntidade (DTO -> Entity)
    public String yearMonthToString(YearMonth validade) {
        return validade != null ? validade.format(formatter) : null;
    }

    @Named("stringToYearMonth") // Usado no paraDTO (Entity -> DTO)
    public YearMonth stringToYearMonth(String validade) {
        if (validade == null || validade.isBlank())
            return null;
        try {
            return YearMonth.parse(validade, formatter);
        } catch (Exception e) {
            return YearMonth.parse(validade);
        }
    }
}
