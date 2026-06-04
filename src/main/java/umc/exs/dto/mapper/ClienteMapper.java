package umc.exs.dto.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.livro.Livro;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // CLIENTE → CLIENTE PERFIL RESPONSE
    @Mapping(source = "id", target = "id")
    @Mapping(source = "nome", target = "nome")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "fotoPerfil", target = "fotoPerfil")
    @Mapping(source = "cpf", target = "cpfMascarado")
    @Mapping(source = "datanasc", target = "dataNascimento")
    @Mapping(source = "dataCriacao", target = "dataCadastro")
    @Mapping(source = "ativo", target = "ativo")
    @Mapping(source = "gen", target = "nivel")
    @Mapping(source = "enderecos", target = "enderecos")
    @Mapping(source = "saldoTokens", target = "saldoTokens")
    ClientePerfilResponse toPerfilResponse(Cliente cliente);

    // SIGNUP REQUEST → CLIENTE
    Cliente paraEntidade(SignupRequest request);

    // CONVERSÃO DE LISTA (O MapStruct usará o método individual abaixo para montar esta lista)
    List<VendaResponse.resumo> toVendaResumoList(List<Livro> livros);

    // CONVERSÃO INDIVIDUAL (Essencial para o MapStruct entender como ligar Livro -> resumo)
    VendaResponse.resumo toVendaResumo(Livro livro);
}