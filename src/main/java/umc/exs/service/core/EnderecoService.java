package umc.exs.service.core;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.EnderecoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.EnderecoRepository;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    /**
     * Salva novo ou reutiliza endereço existente.
     * Busca por campos chave (CEP/rua etc).
     * Retorna entidade salva/reutilizada.
     * @param dto dados endereço
     */
    @Transactional
    public Endereco saveOrReuseEndereco(EnderecoDTO dto) {
        Optional<Endereco> enderecoReutilizado = enderecoRepository.findByValueFields(
                dto.getCep(), dto.getRua(), dto.getNumero(), dto.getComplemento(), 
                dto.getBairro(), dto.getCidade(), dto.getEstado());

        if (enderecoReutilizado.isPresent()) {
            return enderecoReutilizado.get();
        }

        Endereco endereco = enderecoMapper.paraEntidade(dto);
        return enderecoRepository.save(endereco);
    }

    /**
     * Remove endereço de cliente específico.
     * Limpa bidirecional, deleta se sem clientes.
     * @param cliente dono
     * @param enderecoId ID
     */
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

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service gerencia endereços clientes (save/reuse/delete).
 * Reutiliza endereços duplicados por campos chave.
 * Transacional, usa EnderecoRepository/Mapper.
 * Bidirecional limpa corretamente M2M Cliente-Endereco.
 */

