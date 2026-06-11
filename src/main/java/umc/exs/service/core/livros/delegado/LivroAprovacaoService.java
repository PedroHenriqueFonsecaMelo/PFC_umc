package umc.exs.service.core.livros.delegado;

import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.dto.request.admin.AdminAprovacaoRequest;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.api.ExternApi;
import umc.exs.service.core.livros.notificacao.LivroNotificacaoService;
import umc.exs.service.core.livros.recompensa.LivroRecompensaService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.LogAuditoriaService;

/**
 * Serviço responsável pela aprovação e rejeição de livros enviados pelos vendedores.
 * Ao aprovar, calcula preço por estado de conservação, credita tokens e notifica o vendedor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAprovacaoService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;

    private final LogAuditoriaService logAuditoria;
    private final EmailFacade emailFacade;

    private final ExternApi googleBooksService;

    private final LivroNotificacaoService livroNotificacaoService;
    private final LivroRecompensaService livroRecompensaService;

    private static final String LOG_LIVRO_APROVADO = "LIVRO_APROVADO";
    private static final String LOG_LIVRO_REJEITADO = "LIVRO_REJEITADO";
    private static final String LOG_LOTE_ATUALIZADO = "LOTE_STATUS_ATUALIZADO";
    private static final String LOG_LIVRO_APROVACAO_PROCESSADA = "LIVRO_APROVACAO_PROCESSADA";

    private static final String MENSAGEM_LIVRO_NAO_ENCONTRADO = "Livro não encontrado";

    // ========================= APROVAÇÃO =========================

    /**
     * Aprova um livro enviado pelo vendedor, define estado, calcula preço e credita tokens de recompensa.
     * Notifica vendedor por e-mail e atualiza o status do lote se todos os livros forem processados.
     */
    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, AdminAprovacaoRequest dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(MENSAGEM_LIVRO_NAO_ENCONTRADO));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());

        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        // PREÇO AUTOMÁTICO — definido pelo estado de conservação informado pelo admin
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());

        if (anuncio.getGenero() == null || anuncio.getGenero().isBlank()) {
            try {
                String genero = googleBooksService.buscarGeneroPorIsbn(anuncio.getIsbn());
                anuncio.setGenero(genero);
            } catch (Exception e) {
                log.warn("GENERO_API_FALHA isbn={} erro={}", anuncio.getIsbn(), e.getMessage());
            }
        }

        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isBlank()) {
            anuncio.setFotosUrls(dto.getFotosUrls());
        }

        Livro saved = livroRepository.save(anuncio);

        livroNotificacaoService.notificarWishlistSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {

            double saldoAntes = vendedor.getSaldoTokens() != null ? vendedor.getSaldoTokens() : 0.0;

            // RECOMPENSA — tokens creditados ao vendedor por cada livro aprovado
            double tokenReward = LivroRecompensaService.TOKEN_REWARD;
            vendedor.setSaldoTokens(saldoAntes + tokenReward);
            clienteRepository.save(vendedor);

            livroRecompensaService.recompensarVendedorPorAprovacao(vendedor);

            logAuditoria.registrarLog(
                    LOG_LIVRO_APROVADO,
                    adminId,
                    vendedor.getEmail(),
                    "livroId=" + livroId + ", vendedorId=" + vendedor.getId()
            );

            try {
                emailFacade.sendHtmlSafe(
                        vendedor.getEmail(),
                        "Livro aprovado",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), tokenReward));
            } catch (Exception e) {
                log.error("EMAIL_APROVACAO_FALHA livroId={} erro={}", livroId, e.getMessage());
            }

            try {
                emailFacade.sendHtmlSafe(
                        vendedor.getEmail(),
                        "Saldo atualizado",
                        EmailHtmlBuilder.atualizacaoSaldo(
                                vendedor.getNome(),
                                saldoAntes,
                                tokenReward,
                                vendedor.getSaldoTokens(),
                                "Aprovação livro",
                                true,
                                LocalDateTime.now()));
            } catch (Exception e) {
                log.error("EMAIL_SALDO_FALHA livroId={} erro={}", livroId, e.getMessage());
            }

            livroNotificacaoService.notificarAprovacaoDashboard(vendedor, anuncio.getTitulo(), tokenReward);
        }

        // APROVAÇÃO — se todos os livros do lote foram processados, atualiza o status do lote
        if (anuncio.getLote() != null) {

            Long loteId = anuncio.getLote().getId();
            long pending = livroRepository.countByLoteIdAndAprovadoFalse(loteId);

            if (pending == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);

                logAuditoria.registrarLog(
                        LOG_LOTE_ATUALIZADO,
                        adminId,
                        null,
                        "loteId=" + loteId + ", status=TOTAL_APROVADO"
                );
            }
        }

        logAuditoria.registrarLog(
                LOG_LIVRO_APROVACAO_PROCESSADA,
                adminId,
                null,
                "livroId=" + livroId + ", status=APROVADO"
        );

        return saved;
    }

    // ========================= REJEIÇÃO =========================

    /**
     * Rejeita um livro enviado pelo vendedor, armazena o motivo e notifica por e-mail.
     * Atualiza o status do lote para REJEITADO ou PARCIAL_APROVADO conforme os demais livros.
     */
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(MENSAGEM_LIVRO_NAO_ENCONTRADO));

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {

            try {
                emailFacade.sendHtmlSafe(
                        vendedor.getEmail(),
                        "Livro rejeitado",
                        EmailHtmlBuilder.livroRejeitado(
                                vendedor.getNome(),
                                anuncio.getTitulo(),
                                comentario));
            } catch (Exception e) {
                log.error("EMAIL_REJEICAO_FALHA livroId={} erro={}", livroId, e.getMessage());
            }

            livroNotificacaoService.notificarRejeicaoDashboard(vendedor, anuncio.getTitulo(), comentario);
        }

        anuncio.setMotivoRejeicao(comentario);
        anuncio.setAdminAprovadorId(adminId);
        livroRepository.save(anuncio);

        Lote lote = anuncio.getLote();

        if (lote != null) {

            long pending = livroRepository
                    .countByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(lote.getId());

            if (pending == 0) {

                long aprovados = livroRepository.findByLoteId(lote.getId()).stream()
                        .filter(l -> Boolean.TRUE.equals(l.getAprovado()))
                        .count();

                lote.setStatus(aprovados == 0
                        ? Lote.LoteStatus.REJEITADO
                        : Lote.LoteStatus.PARCIAL_APROVADO);

                loteRepository.save(lote);

                logAuditoria.registrarLog(
                        LOG_LOTE_ATUALIZADO,
                        adminId,
                        null,
                        "loteId=" + lote.getId() + ", status=" + lote.getStatus()
                );
            }
        }

        logAuditoria.registrarLog(
                LOG_LIVRO_REJEITADO,
                adminId,
                null,
                "livroId=" + livroId + ", motivo=" + comentario
        );
    }
}