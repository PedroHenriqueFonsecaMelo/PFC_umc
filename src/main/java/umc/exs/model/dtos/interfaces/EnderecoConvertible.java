package umc.exs.model.dtos.interfaces;

import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Endereco;

public interface EnderecoConvertible {

        Endereco toEntity();

        EnderecoDTO fromEntity(Endereco endereco);

        // Getters dos campos básicos
        Long getId();

        String getRua();

        String getNumero();

        String getBairro();

        String getCidade();

        String getEstado();

        String getCep();

        String getPais();

        String getComplemento();

        String getTipoResidencia();

}
