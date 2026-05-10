package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.ListaDesejosService;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAdminService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final LogAuditoriaService logAuditoria;
    private final EmailService emailService;
    private final ListaDesejosService listaDesejosService;
    private final GamificacaoService gamificacaoService;

    private static final double TOKEN_REWARD = 10.0;

    public List<Livro> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroRepository.findByLoteId(loteId);
    }

    /**
     * Aprova livro admin define preço/estado.
     * Transfer system, update lote status se completo.
     * 
     * @param livroId ID
     * @param adminId aprovador
     * @param dto     aprovação
     */
    @SuppressWarnings("null")
    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());
        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isBlank()) {
            anuncio.setFotosUrls(dto.getFotosUrls());
        }

        Livro saved = livroRepository.save(anuncio);

        // Notificar clientes interessados via lista de desejos
        try {
            listaDesejosService.notificarClientesSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());
        } catch (Exception e) {
            log.error("Falha ao notificar lista de desejos para ISBN {}: {}", anuncio.getIsbn(), e.getMessage());
        }

        // Identificar o vendedor: campo direto ou via lote
        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            // Creditar tokens ao vendedor apenas na aprovação
            double saldoAntes = vendedor.getSaldoTokens() != null ? vendedor.getSaldoTokens() : 0.0;
            vendedor.setSaldoTokens(saldoAntes + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            // Gamificação: XP ao vendedor
            gamificacaoService.xpLivroAprovado(vendedor.getId());

            logAuditoria.registrarLog("LIVRO_APROVADO_RECOMPENSA", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + livroId + " aprovado - T$" + TOKEN_REWARD + " creditados");

            // E-mail de confirmação ao vendedor
            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Seu livro foi aprovado! — Bibliotroca",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), TOKEN_REWARD));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de aprovação para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }

            // E-mail de atualização de saldo (crédito da recompensa de aprovação)
            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Atualização de saldo — Bibliotroca",
                        EmailHtmlBuilder.atualizacaoSaldo(
                                vendedor.getNome(), saldoAntes, TOKEN_REWARD,
                                vendedor.getSaldoTokens(),
                                "Recompensa pela aprovação do livro: " + anuncio.getTitulo(),
                                true, LocalDateTime.now()));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de saldo para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }
        }

        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();
            long pendingCount = livroRepository.countByLoteIdAndAprovadoFalse(loteId);
            if (pendingCount == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return saved;
    }

    /**
     * Rejeita livro com comentário admin.
     * Set aprovado=false, comentarioAprovacao.
     * Log LIVRO_REJEITADO.
     */
    @SuppressWarnings("null")
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        // E-mail de rejeição ao vendedor (antes de deletar)
        Cliente vendedorRejeicao = anuncio.getVendedor();
        if (vendedorRejeicao == null && anuncio.getLote() != null) {
            vendedorRejeicao = anuncio.getLote().getCliente();
        }
        if (vendedorRejeicao != null) {
            final String emailVendedor = vendedorRejeicao.getEmail();
            final String nomeVendedor = vendedorRejeicao.getNome();
            final String tituloLivro = anuncio.getTitulo();
            try {
                emailService.enviarHtml(
                        emailVendedor,
                        "Livro não aprovado — Bibliotroca",
                        EmailHtmlBuilder.livroRejeitado(nomeVendedor, tituloLivro, comentario));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de rejeição para vendedor {}: {}", emailVendedor, e.getMessage());
            }
        }

        // Captura o lote antes de deletar o livro
        Lote lotePendente = anuncio.getLote();

        livroRepository.delete(anuncio);

        // Atualiza status do lote se não houver mais livros pendentes de revisão
        if (lotePendente != null) {
            long pendingCount = livroRepository.countByLoteIdAndAprovadoFalse(lotePendente.getId());
            if (pendingCount == 0) {
                long approvedCount = livroRepository.findByLoteId(lotePendente.getId()).size();
                Lote.LoteStatus novoStatus = (approvedCount == 0)
                        ? Lote.LoteStatus.REJEITADO
                        : Lote.LoteStatus.PARCIAL_APROVADO;
                lotePendente.setStatus(novoStatus);
                loteRepository.save(lotePendente);
            }
        }

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin#" + adminId,
                "Livro ID " + livroId + " rejeitado pelo administrador.");
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroRepository.deleteById(id);
    }

    /**
     * Adiciona um livro diretamente via painel administrativo.
     * O livro já nasce aprovado (aprovado=true) pois o próprio admin o está inserindo.
     */
    @Transactional
    public Livro adicionarLivroAdmin(String titulo, String autor, String isbn, Double preco,
            EstadoLivro estado, String resumo, Long adminId,
            Boolean emPromocao, Double percentualDesconto, LocalDateTime promocaoExpira) {

        log.info("Admin [id={}] adicionando livro: titulo='{}', autor='{}', isbn='{}'",
                adminId, titulo, autor, isbn);

        boolean promoAtiva = Boolean.TRUE.equals(emPromocao)
                && percentualDesconto != null && percentualDesconto > 0;

        Double precoFinal    = preco;
        Double precoOriginal = null;

        if (promoAtiva) {
            precoOriginal = preco;
            precoFinal    = preco * (1.0 - percentualDesconto / 100.0);
        }

        Livro novoLivro = Livro.builder()
                .titulo(titulo)
                .autor(autor)
                .isbn(isbn)
                .precoAprovado(precoFinal)
                .precoOriginal(precoOriginal)
                .estadoAprovado(estado)
                .resumoOficial(resumo)
                .fotosUrls("[]")
                .aprovado(true)
                .dataAprovacao(LocalDateTime.now())
                .adminAprovadorId(adminId)
                .emPromocao(promoAtiva)
                .promocaoExpira(promoAtiva ? promocaoExpira : null)
                .build();

        Livro saved = livroRepository.save(novoLivro);
        log.info("Livro salvo com sucesso: id={}, aprovado={}, preco={}",
                saved.getId(), saved.getAprovado(), saved.getPrecoAprovado());
        return saved;
    }

    /**
     * Edita um livro existente via painel administrativo.
     */
    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, String titulo, String autor, String isbn,
            Double preco, EstadoLivro estado, String resumo,
            Boolean emPromocao, Double percentualDesconto, LocalDateTime promocaoExpira) {

        log.info("Admin editando livro id={}", id);

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setIsbn(isbn);
        livro.setEstadoAprovado(estado);
        livro.setResumoOficial(resumo);

        boolean promoAtiva = Boolean.TRUE.equals(emPromocao)
                && percentualDesconto != null && percentualDesconto > 0;

        livro.setEmPromocao(promoAtiva);

        if (promoAtiva) {
            livro.setPrecoOriginal(preco);
            livro.setPrecoAprovado(preco * (1.0 - percentualDesconto / 100.0));
            livro.setPromocaoExpira(promocaoExpira);
        } else {
            livro.setPrecoAprovado(preco);
            livro.setPrecoOriginal(null);
            livro.setPromocaoExpira(null);
        }

        Livro saved = livroRepository.save(livro);
        log.info("Livro id={} atualizado com sucesso, preco={}", id, saved.getPrecoAprovado());
        return saved;
    }
}