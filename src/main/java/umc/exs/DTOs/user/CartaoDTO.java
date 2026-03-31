package umc.exs.DTOs.user;

import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartaoDTO {

    private Long id;

    // Número mascarado na saída — ex: **** **** **** 1234
    private String numero;

    private String bandeira;
    private String nomeTitular;

    @DateTimeFormat(pattern = "MM/yyyy")
    private YearMonth validade;

    private String cpfTitular;

    /**
     * CVV NUNCA é retornado em respostas (PCI-DSS).
     * Aceito apenas na entrada de cadastro de cartão.
     * Jackson ignora na serialização (saída JSON).
     */
    @JsonIgnore
    private String cvv;
}
