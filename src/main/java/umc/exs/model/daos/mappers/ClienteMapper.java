package umc.exs.model.daos.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.entidades.usuario.Cliente;

@Mapper(componentModel = "spring", uses = {EnderecoMapper.class, CartaoMapper.class})
public interface ClienteMapper {

    // Converte Signup (Cadastro Inicial) para Entidade
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "saldoTokens", constant = "0.0")
    @Mapping(target = "bloqueada", constant = "false")
    @Mapping(target = "tentativas", constant = "0")
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "cartoes", ignore = true)
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "senha", ignore = true) // Criptografada manualmente no Service
    Cliente toEntity(SignupDTO dto);

    // Converte Entidade para DTO de visualização/perfil
    @Mapping(target = "senha", ignore = true) // Nunca envia a senha para o Frontend
    ClienteDTO toDTO(Cliente cliente);

    // Atualiza Entidade existente (usado no Meu Perfil)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // E-mail geralmente não muda
    @Mapping(target = "saldoTokens", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "bloqueada", ignore = true)
    @Mapping(target = "tentativas", ignore = true)
    void updateEntityFromDto(ClienteDTO dto, @MappingTarget Cliente entity);
}