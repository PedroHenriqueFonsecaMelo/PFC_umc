package umc.exs.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

@Mapper(componentModel = "spring")
public interface EnderecoMapper {

    /**
     * Converte DTO para entidade Endereco.
     * Ignora relação clientes bidirecional.
     * 
     * @param dto dados endereço
     */
    @Mapping(target = "clientes", ignore = true)
    Endereco paraEntidade(EnderecoDTO dto);

    /**
     * Converte entidade para DTO.
     * Completo mapeamento campos.
     * 
     * @param endereco entidade
     */
    EnderecoDTO paraDTO(Endereco endereco);

    /**
     * Atualiza entidade de DTO.
     * Ignora ID e clientes (merge parcial).
     * 
     * @param dto    origem
     * @param entity alvo update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientes", ignore = true)
    void atualizarEntidadeDeDto(EnderecoDTO dto, @MappingTarget Endereco entity);
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Mapper MapStruct Endereco ↔ DTO.
 * paraEntidade: DTO → entidade ignore clientes.
 * paraDTO: entidade → DTO completo.
 * atualizarEntidadeDeDto: merge parcial perfil.
 * Component Spring autowired.
 */
