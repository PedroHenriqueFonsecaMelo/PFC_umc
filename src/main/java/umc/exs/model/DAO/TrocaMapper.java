package umc.exs.model.DAO;

import umc.exs.model.DTO.admin.TrocaDTO;
import umc.exs.model.compras.Troca;

public class TrocaMapper {
    public static TrocaDTO fromEntity(Troca t) {
        if (t == null) return null;
        TrocaDTO dto = new TrocaDTO();
        dto.setId(t.getId());
        dto.setPedidoId(t.getPedidoId());
        dto.setClienteId(t.getClienteId());
        dto.setValor(t.getValor());
        dto.setStatus(t.getStatus());
        dto.setMotivoRejeicao(t.getMotivoRejeicao());
        dto.setDecisaoPor(t.getDecisaoPor());
        dto.setDecisionAt(t.getDecisionAt());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    public static Troca toEntity(TrocaDTO dto) {
        if (dto == null) return null;
        Troca t = new Troca();
        t.setId(dto.getId());
        t.setPedidoId(dto.getPedidoId());
        t.setClienteId(dto.getClienteId());
        t.setValor(dto.getValor());
        t.setStatus(dto.getStatus());
        t.setMotivoRejeicao(dto.getMotivoRejeicao());
        t.setDecisaoPor(dto.getDecisaoPor());
        t.setDecisionAt(dto.getDecisionAt());
        t.setCreatedAt(dto.getCreatedAt());
        return t;
    }
}