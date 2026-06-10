package umc.exs.service.core.dashboard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.foundation.Lote;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoteService {

        private final LoteRepository loteRepository;
        private final AppLogger appLogger;

        public List<Lote> listarPendentes() {

                List<Lote> lotes = loteRepository.findByStatus(Lote.LoteStatus.PENDENTE);

                log.info("LOTE_LISTAR_PENDENTES total={}", lotes.size());

                appLogger.info(
                                AcaoAuditoria.CLIENTE_LISTAGEM,
                                null,
                                "ADMIN",
                                "Listagem de lotes pendentes");

                return lotes;
        }

        public List<Lote> listarPendentesComCliente() {

                List<Lote> lotes = loteRepository.findByStatusWithCliente(Lote.LoteStatus.PENDENTE);

                log.info("LOTE_LISTAR_PENDENTES_FETCH_CLIENTE total={}", lotes.size());

                appLogger.info(
                                AcaoAuditoria.CLIENTE_LISTAGEM,
                                null,
                                "ADMIN",
                                "Listagem de lotes pendentes com cliente (fetch join)");

                return lotes;
        }

        public Page<Lote> listarPendentesComClientePaginado(Pageable pageable) {

                // Passa o pageable direto para o repositório
                Page<Lote> lotesPaginados = loteRepository.findByStatusWithCliente(Lote.LoteStatus.PENDENTE, pageable);

                log.info("LOTE_LISTAR_PENDENTES_FETCH_CLIENTE total_na_pagina={}",
                                lotesPaginados.getNumberOfElements());

                appLogger.info(
                                AcaoAuditoria.CLIENTE_LISTAGEM,
                                null,
                                "ADMIN",
                                "Listagem paginada de lotes pendentes com cliente (fetch join)");

                return lotesPaginados;
        }

        public Lote findById(Long id) {

                Lote lote = loteRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

                log.info("LOTE_BUSCAR_ID id={}", id);

                return lote;
        }

        public Lote findByIdComCliente(Long id) {

                Lote lote = loteRepository.findByIdWithCliente(id)
                                .orElseThrow(() -> new RuntimeException("Lote não encontrado"));

                log.info("LOTE_BUSCAR_ID_FETCH_CLIENTE id={}", id);

                return lote;
        }

        public long countPendingByCliente(Long clienteId) {

                long count = loteRepository.countByClienteIdAndStatus(
                                clienteId,
                                Lote.LoteStatus.PENDENTE);

                log.info(
                                "LOTE_COUNT_PENDENTES_CLIENTE clienteId={} total={}",
                                clienteId,
                                count);

                return count;
        }
}