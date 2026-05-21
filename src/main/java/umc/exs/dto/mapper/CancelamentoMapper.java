package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.response.compras.CancelamentoResponse;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;

@Mapper(componentModel = "spring")
public interface CancelamentoMapper {

            @Mapping(source = "id", target = "id")
            @Mapping(source = "pedido.id", target = "pedidoId")
            @Mapping(source = "pedido.tituloLivro", target = "tituloLivro")
            @Mapping(source = "pedido.autorLivro", target = "autorLivro")
            @Mapping(source = "pedido.fotosUrls", target = "fotosUrls")
            @Mapping(source = "pedido.isbnLivro", target = "isbnLivro")
            @Mapping(source = "pedido.precoLivro", target = "precoLivro")
            @Mapping(source = "pedido.dataCompra", target = "dataCompra")
            @Mapping(source = "cliente.nome", target = "clienteNome")
            @Mapping(source = "cliente.email", target = "clienteEmail")
            @Mapping(source = "motivoCategoria", target = "motivoCategoria")
            @Mapping(source = "motivoCategoria.descricao", target = "motivoCategoriaDescricao")
            @Mapping(source = "motivoDescricao", target = "motivoDescricao")
            @Mapping(source = "status", target = "status")
            @Mapping(source = "status.descricao", target = "statusDescricao")
            @Mapping(source = "comentarioAdmin", target = "comentarioAdmin")
            @Mapping(source = "dataSolicitacao", target = "dataSolicitacao")
            @Mapping(source = "dataResposta", target = "dataResposta")
            @Mapping(source = "cliente.saldoTokens", target = "saldoAtualComprador")
    CancelamentoResponse toResponse(SolicitacaoCancelamento entity);

    List<CancelamentoResponse> toResponseList(
            List<SolicitacaoCancelamento> entities);
}
