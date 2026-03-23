package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.model.entidades.usuario.Cliente;

@Mapper(componentModel = "spring", uses = { EnderecoMapper.class, CartaoMapper.class })
public interface ClienteMapper {

    /**
     * Converte Signup cadastro inicial entidade.
     * Config saldo=0, bloqueada=false, ignore cartoes/enderecos/senha.
     * @param dto cadastro
     * @return Cliente nova
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "saldoTokens", constant = "0.0")
    @Mapping(target = "bloqueada", constant = "false")
    @Mapping(target = "tentativas", constant = "0")
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "cartoes", ignore = true)
    @Mapping(target = "enderecos", ignore = true)
    @Mapping(target = "senha", ignore = true)
    Cliente paraEntidade(SignupDTO dto);

    /**
     * Converte entidade para DTO perfil/visualização.
     * Oculta senha frontend.
     * @param cliente origem
     * @return DTO
     */
    @Mapping(target = "senha", ignore = true)
    ClienteDTO paraDTO(Cliente cliente);

    /**
     * Atualiza entidade de DTO perfil.
     * Ignore id/email/senha/saldo/dataCriacao imutáveis.
     * @param dto origem
     * @param entity alvo
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "saldoTokens", ignore = true)
    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "dataCriacao", ignore = true)
    @Mapping(target = "bloqueada", ignore = true)
    @Mapping(target = "tentativas", ignore = true)
    void atualizarEntidadeDeDto(ClienteDTO dto, @MappingTarget Cliente entity);
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Mapper MapStruct Cliente ↔ DTOs (SignupDTO, ClienteDTO).
 * paraEntidade: Cadastro → entidade inicial saldo=0 bloqueada=false.
 * paraDTO: Entidade → visualização oculta senha.
 * atualizarEntidadeDeDto: Update parcial perfil ignore imutáveis.
 * Usa Endereco/Cartao Mappers associados.
 */

