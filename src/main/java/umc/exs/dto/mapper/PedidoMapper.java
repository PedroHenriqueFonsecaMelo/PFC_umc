package umc.exs.dto.mapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import umc.exs.dto.response.compras.PedidoResponse;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.usuario.Endereco;

@Mapper(componentModel = "spring")
public interface PedidoMapper {

    @Mapping(source = "comprador.nome", target = "compradorNome")
    @Mapping(source = "comprador.email", target = "compradorEmail")
    @Mapping(source = "comprador.enderecos", target = "compradorEndereco", qualifiedByName = "enderecosToString")

    @Mapping(source = "statusEnvio", target = "statusEnvioDescricao", qualifiedByName = "statusToDescricao")

    @Mapping(target = "temCancelamentoPendente", ignore = true)
    @Mapping(target = "saldoAposEstorno", ignore = true)

    PedidoResponse toResponse(Pedido entity);

    List<PedidoResponse> toResponseList(List<Pedido> entities);

    @Named("statusToDescricao")
    default String statusToDescricao(umc.exs.model.enums.StatusEnvio status) {
        if (status == null)
            return null;
        return status.name(); // ou status.getDescricao() se existir
    }

    @Named("enderecosToString")
    default String enderecosToString(Set<Endereco> enderecos) {
        if (enderecos == null || enderecos.isEmpty())
            return null;

        return enderecos.stream()
                .map(this::formatEndereco)
                .collect(Collectors.joining(", "));
    }

    default String formatEndereco(Endereco e) {
        return e.getRua() + " " + e.getNumero() + " - " + e.getCidade();
    }
}