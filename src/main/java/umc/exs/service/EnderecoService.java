package umc.exs.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.model.dtos.user.EnderecoDTO;
import umc.exs.model.daos.mappers.EnderecoMapper;
import umc.exs.model.daos.repository.EnderecoRepository;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper; // Injetado

    @Transactional
    public Endereco saveOrReuseEndereco(EnderecoDTO dto) {
        Optional<Endereco> enderecoReutilizado = enderecoRepository.findByValueFields(
                dto.getCep(), dto.getRua(), dto.getNumero(), dto.getComplemento(), 
                dto.getBairro(), dto.getCidade(), dto.getEstado());

        if (enderecoReutilizado.isPresent()) {
            return enderecoReutilizado.get();
        }

        // Converte usando o mapper injetado
        Endereco endereco = enderecoMapper.toEntity(dto);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public void deletarEnderecoDoCliente(Cliente cliente, Long enderecoId) {
        Endereco enderecoParaRemover = null;

        for (Endereco e : cliente.getEnderecos()) {
            if (e.getId().equals(enderecoId)) {
                enderecoParaRemover = e;
                break;
            }
        }

        if (enderecoParaRemover == null) {
            throw new IllegalArgumentException("Endereço não encontrado ou não pertence ao cliente.");
        }

        cliente.getEnderecos().remove(enderecoParaRemover);
        enderecoParaRemover.getClientes().remove(cliente);

        if (enderecoParaRemover.getClientes().isEmpty()) {
            enderecoRepository.delete(enderecoParaRemover);
        }
    }
}