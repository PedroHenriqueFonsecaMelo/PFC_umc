package umc.exs.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service
public class EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Transactional
    public Endereco saveOrReuseEndereco(EnderecoDTO dto) {
        Optional<Endereco> enderecoReutilizado = enderecoRepository.findByValueFields(
                dto.getCep(), dto.getRua(), dto.getNumero(), dto.getComplemento(), dto.getBairro(), dto.getCidade(), dto.getEstado());

        if (enderecoReutilizado.isPresent()) {
            return enderecoReutilizado.get();
        }

        Endereco endereco = EnderecoMapper.toEntity(dto);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public void deletarEnderecoDoCliente(Cliente cliente, Long enderecoId) {
        Endereco endereco = cliente.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Endereço não encontrado ou não pertence ao cliente."));

        cliente.getEnderecos().remove(endereco);
        endereco.getClientes().remove(cliente);

        if (endereco.getClientes().isEmpty()) {
            enderecoRepository.delete(endereco);
        }
    }
}
