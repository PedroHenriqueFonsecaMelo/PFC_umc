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

    private static final String MENSAGEM_LIVRO_NAO_ENCONTRADO = "Livro não encontrado";

    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, AdminAprovacaoRequest dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(MENSAGEM_LIVRO_NAO_ENCONTRADO));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());

        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());

        // Busca gênero automaticamente via Google Books se não estiver preenchido
        if (anuncio.getGenero() == null || anuncio.getGenero().isBlank()) {
            try {
                String genero = googleBooksService.buscarGeneroPorIsbn(anuncio.getIsbn());
                if (genero != null) {
                    anuncio.setGenero(genero);
                }
            } catch (Exception e) {
                log.warn("Não foi possível buscar gênero para ISBN {}: {}", anuncio.getIsbn(), e.getMessage());
            }
        }

        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isBlank()) {
            anuncio.setFotosUrls(dto.getFotosUrls());
        }

        Livro saved = livroRepository.save(anuncio);

        // Notificações wishlist
        livroNotificacaoService.notificarWishlistSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {

            double saldoAntes = vendedor.getSaldoTokens() != null ? vendedor.getSaldoTokens() : 0.0;

            // reward (tokens)
            double tokenReward = LivroRecompensaService.TOKEN_REWARD;
            vendedor.setSaldoTokens(saldoAntes + tokenReward);
            clienteRepository.save(vendedor);

            // gamificação (XP)
            livroRecompensaService.recompensarVendedorPorAprovacao(vendedor);

            logAuditoria.registrarLog(
                    "LIVRO_APROVADO",
                    vendedor.getId(),
                    vendedor.getEmail(),
                    "Livro " + livroId);

            try {
                emailFacade.sendHtmlSafe(
                        vendedor.getEmail(),
                        "Livro aprovado",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), tokenReward));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
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
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
            }

            // Notificação dashboard: livro aprovado
            livroNotificacaoService.notificarAprovacaoDashboard(vendedor, anuncio.getTitulo(), tokenReward);
        }

        // Atualiza status do lote
        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();

            long pending = livroRepository.countByLoteIdAndAprovadoFalse(loteId);

            if (pending == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return saved;
    }

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
                log.error("Erro ao enviar email de livro rejeitado: {}", e.getMessage(), e);
            }

            livroNotificacaoService.notificarRejeicaoDashboard(vendedor, anuncio.getTitulo(), comentario);
        }

        Lote lote = anuncio.getLote();

        anuncio.setMotivoRejeicao(comentario);
        anuncio.setAdminAprovadorId(adminId);
        livroRepository.save(anuncio);

        if (lote != null) {
            // Conta apenas livros ainda sem decisão (exclui rejeitados e aprovados)
            long pending = livroRepository
                    .countByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(lote.getId());

            if (pending == 0) {
                // Conta apenas os aprovados para definir o status final do lote
                long aprovados = livroRepository.findByLoteId(lote.getId()).stream()
                        .filter(l -> Boolean.TRUE.equals(l.getAprovado()))
                        .count();

                lote.setStatus(aprovados == 0
                        ? Lote.LoteStatus.REJEITADO
                        : Lote.LoteStatus.PARCIAL_APROVADO);

                loteRepository.save(lote);
            }

            logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin", "Livro " + livroId);
        }
    }
}
