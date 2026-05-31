package umc.exs.service.cliente;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.usuario.EnderecoRepository;

@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;

    /**
     * Reutiliza endereço existente
     * ou cria novo.
     */
    @Transactional
    public Endereco saveOrReuseEndereco(Endereco dto) {

        Optional<Endereco> enderecoExistente = enderecoRepository.findByValueFields(
                dto.getCep(),
                dto.getRua(),
                dto.getNumero(),
                dto.getComplemento(),
                dto.getBairro(),
                dto.getCidade(),
                dto.getEstado());

        if (enderecoExistente.isPresent()) {
            return enderecoExistente.get();
        }

        return enderecoRepository.save(dto);
    }

    /**
     * Vincula endereço ao cliente.
     */
    @Transactional
    public void vincularNovoEndereco(
            Cliente cliente,
            Endereco dto) {

        Endereco endereco = saveOrReuseEndereco(dto);

        // evita duplicidade
        if (!cliente.getEnderecos().contains(endereco)) {

            cliente.getEnderecos().add(endereco);

            endereco.getClientes().add(cliente);
        }
    }

    /**
     * Remove vínculo cliente-endereço.
     *
     * Remove o endereço do banco
     * somente se ele ficar órfão.
     */
    @Transactional
    public void deletarEnderecoDoCliente(
            Cliente cliente,
            Long enderecoId) {

        Endereco endereco = enderecoRepository.findById(enderecoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Endereço não encontrado"));

        if (!cliente.getEnderecos().contains(endereco)) {

            throw new IllegalArgumentException(
                    "Endereço não pertence ao cliente.");
        }

        // remove vínculo bidirecional
        cliente.getEnderecos().remove(endereco);

        endereco.getClientes().remove(cliente);

        // remove entidade órfã
        if (endereco.getClientes().isEmpty()) {

            enderecoRepository.delete(endereco);
        }
    }

    /**
     * Sincroniza os endereços do cliente.
     *
     * Regras:
     * - remove vínculos ausentes
     * - mantém existentes
     * - adiciona novos
     * - reutiliza endereços iguais
     */

    @Transactional
    public void sincronizarEnderecos(
            Cliente cliente,
            List<Endereco> dtos) {

        if (dtos == null) {
            return;
        }

        /**
         * IDs recebidos.
         */
        Set<Long> idsRecebidos = dtos.stream()
                .map(Endereco::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        /**
         * Snapshot evita ConcurrentModificationException
         */
        Set<Endereco> enderecosAtuais = new HashSet<>(cliente.getEnderecos());

        /**
         * 1. REMOVE VÍNCULOS AUSENTES
         */
        for (Endereco endereco : enderecosAtuais) {

            if (endereco.getId() != null &&
                    !idsRecebidos.contains(endereco.getId())) {

                cliente.getEnderecos().remove(endereco);

                endereco.getClientes().remove(cliente);

                /**
                 * Remove endereço órfão.
                 */
                if (endereco.getClientes().isEmpty()) {

                    enderecoRepository.delete(endereco);
                }
            }
        }

        /**
         * 2. PROCESSA DTOs
         */
        for (Endereco dto : dtos) {

            /**
             * ENDEREÇO EXISTENTE
             */
            if (dto.getId() != null &&
                    dto.getId() != 0) {

                Endereco existente = enderecoRepository.findById(dto.getId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Endereço não encontrado"));

                /**
                 * Segurança:
                 * cliente deve possuir vínculo.
                 */
                if (!existente.getClientes()
                        .contains(cliente)) {

                    throw new IllegalArgumentException(
                            "Endereço ID "
                                    + dto.getId()
                                    + " não pertence ao cliente.");
                }

                /**
                 * Atualização.
                 *
                 * IMPORTANTE:
                 * Isso afeta TODOS os clientes
                 * vinculados ao endereço.
                 */

                existente.setPais(dto.getPais());
                existente.setCep(dto.getCep());
                existente.setEstado(dto.getEstado());
                existente.setCidade(dto.getCidade());
                existente.setRua(dto.getRua());
                existente.setBairro(dto.getBairro());
                existente.setNumero(dto.getNumero());
                existente.setComplemento(dto.getComplemento());
                existente.setTipoResidencia(dto.getTipoResidencia());

            } else {

                /**
                 * NOVO ENDEREÇO
                 */
                vincularNovoEndereco(cliente, dto);
            }
        }
    }
}