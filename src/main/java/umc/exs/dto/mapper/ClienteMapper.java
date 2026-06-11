package umc.exs.dto.mapper;

import java.util.List;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.request.cliente.EnderecoShared;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.model.entidades.livro.Livro;

/**
 * Mapper MapStruct que converte entidades de Cliente para DTOs de resposta e vice-versa.
 * Cobre mapeamentos de perfil, cadastro, vendas e endereços do cliente.
 */
@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // =========================
    // ENTITY -> PERFIL RESPONSE
    // =========================

    /**
     * Converte um Cliente para ClientePerfilResponse com os dados básicos do perfil.
     * Campos derivados (totais, pedidos, cancelamentos) são ignorados aqui e preenchidos pelo service após o mapeamento.
     */
    @Mapping(source = "datanasc", target = "dataNascimento")
    @Mapping(source = "dataCriacao", target = "dataCadastro")
    @Mapping(source = "gen", target = "nivel")
    @Mapping(source = "cpf", target = "cpfMascarado")
    @Mapping(source = "enderecos", target = "enderecos")

    // campos derivados (NÃO existem no entity → service preenche depois)
    @Mapping(target = "totalGasto", ignore = true)
    @Mapping(target = "totalRecarregado", ignore = true)
    @Mapping(target = "quantidadeCuponsUsados", ignore = true)
    @Mapping(target = "totalPedidos", ignore = true)
    @Mapping(target = "totalCancelamentos", ignore = true)
    @Mapping(target = "pedidos", ignore = true)
    @Mapping(target = "totalLivrosVendidos", ignore = true)
    @Mapping(target = "totalLotesEnviados", ignore = true)
    @Mapping(target = "totalLivrosRejeitados", ignore = true)
    @Mapping(target = "totalTopicosForum", ignore = true)
    @Mapping(target = "totalListaDesejos", ignore = true)

    ClientePerfilResponse toPerfilResponse(Cliente cliente);

    // =========================
    // SIGNUP -> ENTITY
    // =========================

    /**
     * Converte um SignupRequest para a entidade Cliente no momento do cadastro.
     * O mapeamento é direto, sem transformações adicionais.
     */
    Cliente paraEntidade(SignupRequest request);

    // =========================
    // LIVRO -> VENDA RESUMO
    // =========================

    /**
     * Converte uma lista de Livros para uma lista de resumos de venda do vendedor.
     * Retorna lista vazia caso a entrada seja nula.
     */
    default List<VendaResponse.Resumo> toVendaResumoList(List<Livro> livros) {
        if (livros == null) return List.of();
        return livros.stream().map(this::toVendaResumo).toList();
    }

    /**
     * Converte um Livro para o resumo de venda exibido na tela de Minhas Vendas do vendedor.
     * Mapeamento gerado automaticamente pelo MapStruct.
     */
    VendaResponse.Resumo toVendaResumo(Livro livro);

    // =========================
    // ENDEREÇO MAPPING
    // =========================

    /**
     * Converte uma entidade Endereco para o DTO compartilhado EnderecoShared.
     * Utilizado ao retornar endereços do cliente na resposta da API.
     */
    EnderecoShared toEnderecoDTO(Endereco endereco);

    /**
     * Converte um DTO EnderecoShared para a entidade Endereco.
     * Utilizado ao salvar ou atualizar um endereço do cliente.
     */
    Endereco toEntity(EnderecoShared dto);

    /**
     * Converte um Set de entidades Endereco para uma List de DTOs EnderecoShared.
     * Usado para serializar os endereços do cliente na resposta de perfil.
     */
    List<EnderecoShared> toEnderecoDTOList(Set<Endereco> enderecos);

    /**
     * Converte uma List de DTOs EnderecoShared para uma List de entidades Endereco.
     * Utilizado ao processar múltiplos endereços recebidos via requisição.
     */
    List<Endereco> toEntityList(List<EnderecoShared> dtos);
}
