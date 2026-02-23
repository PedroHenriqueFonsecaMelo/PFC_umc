package umc.exs.model.dtos.user;

import java.util.ArrayList;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = { "senha", "enderecos", "cartoes" })
public class ClienteDTO { 

    private Long id;
    private String nome;
    private String email;
    private String datanasc;
    private String gen;
    private String senha;
    private String cpf;
    private Double saldoTokens;

    private List<EnderecoDTO> enderecos = new ArrayList<>();
    private List<CartaoDTO> cartoes = new ArrayList<>();
}