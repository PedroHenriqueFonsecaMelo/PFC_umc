package umc.exs.DTOs.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateDTO {

    private String nome;
    private String datanasc;
    private String senha;

    private List<EnderecoDTO> enderecos;

}