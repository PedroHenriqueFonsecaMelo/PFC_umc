package umc.exs.converter;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

/**
 * Converte datas entre YearMonth e String no formato MM/yy para persistência da validade de cartões.
 * Utilizado pelo MapStruct para mapear o campo de validade entre entidade e DTO.
 */
@Component
public class DateMapper {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");

    /**
     * Converte um YearMonth para String no formato MM/yy ao salvar no banco de dados.
     * Retorna null caso a validade seja nula.
     */
    @Named("yearMonthToString") // Usado no paraEntidade (DTO -> Entity)
    public String yearMonthToString(YearMonth validade) {
        return validade != null ? validade.format(formatter) : null;
    }

    /**
     * Converte uma String no formato MM/yy para YearMonth ao ler do banco de dados.
     * Em caso de falha no formato padrão, tenta um parse alternativo antes de lançar exceção.
     */
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
