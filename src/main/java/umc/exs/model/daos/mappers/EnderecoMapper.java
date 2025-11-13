package umc.exs.model.daos.mappers;

import umc.exs.model.dtos.interfaces.EnderecoConvertible;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

public class EnderecoMapper {

    /**
     * Converte Entidade Endereco para EnderecoDTO.
     */
    public static EnderecoDTO fromEntity(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        EnderecoDTO dto = new EnderecoDTO();
        dto.setId(endereco.getId());
        dto.setRua(endereco.getRua());
        dto.setNumero(endereco.getNumero());
        dto.setBairro(endereco.getBairro());
        dto.setCidade(endereco.getCidade());
        dto.setEstado(endereco.getEstado());
        dto.setCep(endereco.getCep());
        dto.setPais(endereco.getPais());
        dto.setComplemento(endereco.getComplemento());
        dto.setTipoResidencia(endereco.getTipoResidencia());
        
        return dto;
    }

    /**
     * Converte EnderecoConvertible (DTO) para Entidade Endereco (cria uma nova).
     */
    public static Endereco toEntity(EnderecoConvertible dto) {
        if (dto == null) {
            return null;
        }
        Endereco e = new Endereco();
        // Não é necessário setar o ID aqui, pois o objeto é novo. O banco de dados cuidará disso.
        // Se o DTO tem um ID, ele será ignorado neste método de "criação".
        
        // Chamada ao novo método de atualização de campos
        return updateEntityFromDto(e, dto);
    }
    
    // --- NOVO MÉTODO PARA ATUALIZAÇÃO ---

    /**
     * Atualiza uma entidade Endereco existente com dados de um DTO.
     * * @param entity A entidade Endereco a ser atualizada.
     * @param dto O DTO contendo os novos dados.
     * @return A entidade Endereco atualizada.
     */
    public static Endereco updateEntityFromDto(Endereco entity, EnderecoConvertible dto) {
        if (entity == null || dto == null) {
            // Em cenários reais, isso deve lançar uma exceção
            return entity; 
        }

        // Não atualizamos o ID, pois ele é a chave primária da entidade.
        // entity.setId(dto.getId()); 
        
        // Atualiza todos os campos editáveis
        entity.setRua(dto.getRua());
        entity.setNumero(dto.getNumero());
        entity.setBairro(dto.getBairro());
        entity.setCidade(dto.getCidade());
        entity.setEstado(dto.getEstado());
        entity.setCep(dto.getCep());
        entity.setPais(dto.getPais());
        entity.setComplemento(dto.getComplemento());
        entity.setTipoResidencia(dto.getTipoResidencia());
        
        return entity;
    }
}