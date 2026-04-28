package umc.exs.service.core.cliente;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.mappers.EnderecoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.EnderecoRepository;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final EnderecoMapper enderecoMapper;

    /**
     * Salva novo ou reutiliza endereço existente.
     * Busca por campos chave (CEP/rua etc).
     * Retorna entidade salva/reutilizada.
     * 
     * @param dto dados endereço
     */
    @SuppressWarnings("null")
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
     * 
     * @param cliente    dono
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

    @Transactional
    public void vincularNovoEndereco(Cliente cliente, EnderecoDTO dto) {
        Endereco endereco = saveOrReuseEndereco(dto);
        if (!cliente.getEnderecos().contains(endereco)) {
            cliente.getEnderecos().add(endereco);
            endereco.getClientes().add(cliente);
        }
    }

    @SuppressWarnings("null")
    @Transactional
    public void sincronizarEnderecos(Cliente cliente, List<EnderecoDTO> dtos) {
        if (dtos == null) return;

        // 1. Identificar IDs que devem permanecer
        Set<Long> idsManter = dtos.stream()
                .map(EnderecoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        // 2. Remover associações que não estão no DTO (Orfãos)
        cliente.getEnderecos().removeIf(endereco -> {
            if (!idsManter.contains(endereco.getId())) {
                endereco.getClientes().remove(cliente);
                return true;
            }
            return false;
        });

        // 3. Adicionar novos ou atualizar existentes
        for (EnderecoDTO dto : dtos) {
            if (dto.getId() != null && dto.getId() != 0) {
                Endereco existente = enderecoRepository.findById(dto.getId())
                        .orElseThrow(() -> new EntityNotFoundException("Endereço não encontrado"));
                enderecoMapper.atualizarEntidadeDeDto(dto, existente);
            } else {
                vincularNovoEndereco(cliente, dto);
            }
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
