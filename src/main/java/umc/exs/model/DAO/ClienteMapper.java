package umc.exs.model.DAO;

import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.DTO.user.ClienteDTO;
import umc.exs.model.entidades.Cliente;

public class ClienteMapper {

    public static ClienteDTO fromEntity(Cliente c) {
        if (c == null) return null;
        ClienteDTO dto = new ClienteDTO();
        dto.setId(c.getId());
        dto.setNome(c.getNome());
        dto.setEmail(c.getEmail());
        dto.setDatanasc(c.getDatanasc());
        dto.setGen(c.getGen());
        // do not expose senha
        return dto;
    }

    public static Cliente toEntity(ClienteDTO dto) {
        if (dto == null) return null;
        Cliente c = new Cliente();
        if (dto.getId() != null) c.setId(dto.getId());
        c.setNome(dto.getNome());
        c.setEmail(dto.getEmail());
        c.setDatanasc(dto.getDatanasc());
        c.setGen(dto.getGen());
        return c;
    }

    // usado no processo de signup (LoginDTO -> Cliente)
    public static Cliente toEntityFromSignup(SignupDTO signup) {
        if (signup == null) return null;
        Cliente c = new Cliente();
        c.setEmail(signup.getEmail());
        c.setNome(signup.getNome());
        c.setDatanasc(signup.getDatanasc());
        c.setGen(signup.getGen());
        c.setSenha(signup.getSenha());
        return c;
    }
}