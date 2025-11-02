package umc.exs.model.DAO;

import umc.exs.model.DTO.admin.AdminDTO;
import umc.exs.model.foundation.Administrador;

public class AdminMapper {
    public static AdminDTO fromEntity(Administrador e) {
        if (e == null) return null;
        AdminDTO dto = new AdminDTO();
        dto.setId(e.getId());
        dto.setNome(e.getNome());
        dto.setEmail(e.getEmail());
        dto.setPassword(e.getPassword());
        return dto;
    }

    public static Administrador toEntity(AdminDTO dto) {
        if (dto == null) return null;
        Administrador e = new Administrador();
        e.setId(dto.getId());
        e.setNome(dto.getNome());
        e.setEmail(dto.getEmail());
        e.setPassword(dto.getPassword());
        return e;
    }
}