package umc.exs.model.dtos.user;

import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartaoDTO {

    private Long id;
    private String numero; 
    private String bandeira;
    private String nomeTitular;

    @DateTimeFormat(pattern = "MM/yyyy")
    private YearMonth validade; 
    
    private String cpfTitular; 
    
    // CV se for usado apenas no cadastro
    private String cvv; 
}