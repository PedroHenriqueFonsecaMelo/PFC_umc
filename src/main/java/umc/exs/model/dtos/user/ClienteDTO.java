package umc.exs.model.dtos.user;

import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import umc.exs.model.daos.mappers.ClienteMapper; // Importado para delegação
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.entidades.usuario.Cliente;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString(exclude = { "senha", "enderecos", "cartoes" })
public class ClienteDTO implements ClienteConvertible {

    private Long id;
    private String nome;
    private String email;
    private String datanasc;
    private String gen;
    private String senha;
    private String cpf;

    private List<EnderecoDTO> enderecos = new ArrayList<>();
    private List<CartaoDTO> cartoes = new ArrayList<>();

    // --- MÉTODOS DE CONVERSÃO DELEGADOS ---

    // 1. fromEntity (Entidade -> DTO): Usado em Mappers/Services para criar um DTO
    @Override
    public ClienteDTO fromEntity(Cliente cliente) {
        return ClienteMapper.fromEntity(cliente);
    }

    // 2. toEntity (DTO -> Entidade): Usado para satisfazer a interface
    // ClienteConvertible
    @Override
    public Cliente toEntity() {
        // Delega toda a lógica de mapeamento (como List -> Set) ao Mapper
        return ClienteMapper.toEntity(this);
    }
}