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

/**
 * Mapper MapStruct que converte a entidade Pedido para PedidoResponse.
 * Inclui dados do comprador, endereço formatado e descrição do status de envio.
 */
@Mapper(componentModel = "spring")
public interface PedidoMapper {

    /**
     * Converte um Pedido para o DTO de resposta com dados do comprador e status de envio.
     * Os campos temCancelamentoPendente e saldoAposEstorno são ignorados aqui e preenchidos pelo service.
     */
    @Mapping(source = "comprador.nome", target = "compradorNome")
    @Mapping(source = "comprador.email", target = "compradorEmail")
    @Mapping(source = "comprador.enderecos", target = "compradorEndereco", qualifiedByName = "enderecosToString")

    @Mapping(source = "statusEnvio", target = "statusEnvioDescricao", qualifiedByName = "statusToDescricao")

    @Mapping(target = "temCancelamentoPendente", ignore = true)
    @Mapping(target = "saldoAposEstorno", ignore = true)

    PedidoResponse toResponse(Pedido entity);

    /**
     * Converte uma lista de pedidos para uma lista de DTOs de resposta.
     * Aplica o mesmo mapeamento de toResponse() para cada item da lista.
     */
    List<PedidoResponse> toResponseList(List<Pedido> entities);

    /**
     * Converte o enum StatusEnvio para uma String descritiva do status.
     * Retorna null se o status for nulo.
     */
    @Named("statusToDescricao")
    default String statusToDescricao(umc.exs.model.enums.StatusEnvio status) {
        if (status == null)
            return null;
        return status.name(); // ou status.getDescricao() se existir
    }

    /**
     * Converte o conjunto de endereços do comprador para uma String formatada com todos os endereços separados por vírgula.
     * Retorna null caso o conjunto seja nulo ou vazio.
     */
    @Named("enderecosToString")
    default String enderecosToString(Set<Endereco> enderecos) {
        if (enderecos == null || enderecos.isEmpty())
            return null;

        return enderecos.stream()
                .map(this::formatEndereco)
                .collect(Collectors.joining(", "));
    }

    /**
     * Formata um endereço no padrão "Rua Número - Cidade".
     * Utilizado pelo enderecosToString() para formatar cada endereço individualmente.
     */
    default String formatEndereco(Endereco e) {
        return e.getRua() + " " + e.getNumero() + " - " + e.getCidade();
    }
}
