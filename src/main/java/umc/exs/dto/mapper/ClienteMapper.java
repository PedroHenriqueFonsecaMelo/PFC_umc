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

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // =========================
    // ENTITY -> PERFIL RESPONSE
    // =========================
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
    Cliente paraEntidade(SignupRequest request);

    // =========================
    // LIVRO -> VENDA RESUMO
    // =========================
    default List<VendaResponse.Resumo> toVendaResumoList(List<Livro> livros) {
        if (livros == null) return List.of();
        return livros.stream().map(this::toVendaResumo).toList();
    }

    VendaResponse.Resumo toVendaResumo(Livro livro);

    // =========================
    // ENDEREÇO MAPPING
    // =========================
    EnderecoShared toEnderecoDTO(Endereco endereco);

    Endereco toEntity(EnderecoShared dto);

    List<EnderecoShared> toEnderecoDTOList(Set<Endereco> enderecos);

    List<Endereco> toEntityList(List<EnderecoShared> dtos);
}