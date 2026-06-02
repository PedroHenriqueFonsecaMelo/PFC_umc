package umc.exs.dto.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.livro.Livro;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    // CLIENTE → CLIENTE PERFIL RESPONSE
    // (mantém valores que já existem na entidade; campos agregados devem ser
    // preenchidos em outro lugar)
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

    // TRANSACAO → reaproveitando lista (sem mapeamento extra)
    List<Transacao> toTransacaoList(List<Transacao> transacoes);

    // ENDEREÇO → reaproveitando lista
    List<Endereco> toEnderecoList(List<Endereco> enderecos);

    // VENDAS (Livro → VendaResponse.resumo)
    List<VendaResponse.resumo> toVendaResumoList(List<Livro> livros);

    // SIGNUP REQUEST → CLIENTE
    Cliente paraEntidade(SignupRequest request);
}
