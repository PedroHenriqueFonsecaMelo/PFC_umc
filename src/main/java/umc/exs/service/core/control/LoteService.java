package umc.exs.service.core.control;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.foundation.Lote;
import umc.exs.repository.negocios.LoteRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoteService {
    private final LoteRepository loteRepository;

    /**
     * Lista lotes com status PENDENTE.
     * Para aprovação admin.
     * 
     * @return lista Lote pendentes
     */
    public List<Lote> listarPendentes() {
        return loteRepository.findByStatus(Lote.LoteStatus.PENDENTE);
    }

    /**
     * Busca lote por ID.
     * Throw se não encontrado.
     * 
     * @param id lote
     * @return Lote
     */
    @SuppressWarnings("null")
    public Lote findById(Long id) {
        return loteRepository.findById(id).orElseThrow(() -> new RuntimeException("Lote não encontrado"));
    }

    /**
     * Conta pendentes por cliente ID.
     * Limite cadastro lotes usuário.
     * 
     * @param clienteId
     * @return count
     */
    public long countPendingByCliente(Long clienteId) {
        return loteRepository.countByClienteIdAndStatus(clienteId, Lote.LoteStatus.PENDENTE);
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service operações Lote (pacote livros venda).
 * Lista pendentes admin, busca ID, count cliente.
 * Usa LoteRepository Spring Data.
 * Status PENDENTE/TOTAL_APROVADO enum.
 */
