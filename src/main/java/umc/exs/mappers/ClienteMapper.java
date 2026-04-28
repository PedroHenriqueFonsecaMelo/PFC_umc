package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.model.entidades.usuario.Cliente;

@Mapper(componentModel = "spring", uses = { EnderecoMapper.class, CartaoMapper.class })
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "saldoTokens", constant = "0.0")
    @Mapping(target = "bloqueada", constant = "false")
    @Mapping(target = "tentativas", constant = "0")
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "cartoes", ignore = true)
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "avaliacoes", ignore = true)
    @Mapping(target = "fotoPerfil", ignore = true)
    Cliente paraEntidade(SignupDTO dto);

    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "saldoTokens", source = "saldoTokens")
    @Mapping(target = "cpf", expression = "java(umc.exs.service.senha.FieldValidation.mascararCpf(cliente.getCpf()))")
    ClienteDTO paraDTO(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "saldoTokens", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "bloqueada", ignore = true)
    @Mapping(target = "tentativas", ignore = true)
    @Mapping(target = "avaliacoes", ignore = true)
    void atualizarEntidadeDeDto(ClienteDTO dto, @MappingTarget Cliente entity);
}