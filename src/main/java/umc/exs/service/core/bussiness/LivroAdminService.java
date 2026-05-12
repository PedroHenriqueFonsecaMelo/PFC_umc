package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.admin.AdminAprovacaoDTO;
import umc.exs.dtos.admin.LivroAdminRequest;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.mappers.LivroMapper;
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
import umc.exs.service.notificacao.NotificacaoService;

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
    private final NotificacaoService notificacaoService;

    private final LivroMapper livroMapper;

    private static final double TOKEN_REWARD = 10.0;
    private String message = "Livro não encontrado";

    // ========================= LISTAGENS =========================

    public List<LivroDTO> listarLivrosPendentes() {
        List<Livro> livros = livroRepository.findByAprovadoFalse();
        return converterLista(livros);
    }

    public List<LivroDTO> listarLivrosAprovados() {
        List<Livro> livros = livroRepository.findByAprovadoTrue();
        return converterLista(livros);
    }

    public List<LivroDTO> listarLivrosPorLote(Long loteId) {
        List<Livro> livros = livroRepository.findByLoteId(loteId);
        return converterLista(livros);
    }

    // ========================= APROVAÇÃO =========================

    @SuppressWarnings("null")
    @Transactional
    public LivroDTO aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(message));

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

        // NOTIFICAÇÕES
        try {
            listaDesejosService.notificarClientesSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());
        } catch (Exception e) {
            log.error("Erro wishlist: {}", e.getMessage());
        }

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            double saldoAntes = vendedor.getSaldoTokens() != null ? vendedor.getSaldoTokens() : 0.0;

            vendedor.setSaldoTokens(saldoAntes + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            gamificacaoService.xpLivroAprovado(vendedor.getId());

            logAuditoria.registrarLog("LIVRO_APROVADO", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + livroId);

            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Livro aprovado",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), TOKEN_REWARD));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
            }

            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Saldo atualizado",
                        EmailHtmlBuilder.atualizacaoSaldo(
                                vendedor.getNome(),
                                saldoAntes,
                                TOKEN_REWARD,
                                vendedor.getSaldoTokens(),
                                "Aprovação livro",
                                true,
                                LocalDateTime.now()));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
            }

            // Notificação dashboard: livro aprovado
            try {
                notificacaoService.criarNotificacaoDashboard(
                        vendedor,
                        String.format("Seu livro '%s' foi aprovado e está na vitrine! Você recebeu T$ %.2f de bônus.",
                                anuncio.getTitulo(), TOKEN_REWARD),
                        "/livros/vitrine");
            } catch (Exception e) {
                log.error("Erro ao notificar aprovação para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }
        }

        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();

            long pending = livroRepository.countByLoteIdAndAprovadoFalse(loteId);

            if (pending == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return livroMapper.paraDTO(saved);
    }

    // ========================= REJEIÇÃO =========================

    @SuppressWarnings("null")
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(message));

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Livro rejeitado",
                        EmailHtmlBuilder.livroRejeitado(
                                vendedor.getNome(),
                                anuncio.getTitulo(),
                                comentario));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro rejeitado: {}", e.getMessage(), e);
            }

            // Notificação dashboard: livro rejeitado
            try {
                String motivoTexto = (comentario != null && !comentario.isBlank())
                        ? " Motivo: " + comentario
                        : "";
                notificacaoService.criarNotificacaoDashboard(
                        vendedor,
                        String.format("Seu livro '%s' foi rejeitado.%s", anuncio.getTitulo(), motivoTexto),
                        "/clientes/homepage");
            } catch (Exception e) {
                log.error("Erro ao notificar rejeição para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }
        }

        Lote lote = anuncio.getLote();

        livroRepository.delete(anuncio);

        if (lote != null) {
            long pending = livroRepository.countByLoteIdAndAprovadoFalse(lote.getId());

            if (pending == 0) {
                long aprovados = livroRepository.findByLoteId(lote.getId()).size();

                lote.setStatus(aprovados == 0
                        ? Lote.LoteStatus.REJEITADO
                        : Lote.LoteStatus.PARCIAL_APROVADO);

                loteRepository.save(lote);
            }
        }

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin",
                "Livro " + livroId);
    }

    // ========================= CRUD ADMIN =========================

    @SuppressWarnings("null")
    @Transactional
    public LivroDTO adicionarLivroAdmin(LivroAdminRequest req) {

        // ===== buscar vendedor =====
        Cliente vendedor = null;

        if (req.getVendedorId() != null) {
            vendedor = clienteRepository.findById(req.getVendedorId())
                    .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));
        }

        // ===== regra de promoção =====
        boolean promoAtiva = Boolean.TRUE.equals(req.getEmPromocao())
                && req.getPercentualDesconto() != null
                && req.getPercentualDesconto() > 0;

        Double precoFinal = req.getPreco();
        Double precoOriginal = null;

        if (promoAtiva) {
            precoOriginal = req.getPreco();
            precoFinal = req.getPreco() * (1.0 - req.getPercentualDesconto() / 100.0);
        }

        // ===== criação =====
        Livro livro = Livro.builder()
                .titulo(req.getTitulo())
                .autor(req.getAutor())
                .isbn(req.getIsbn())
                .precoAprovado(precoFinal)
                .precoOriginal(precoOriginal)
                .estadoAprovado(req.getEstado())
                .resumoOficial(req.getResumo())
                .fotosUrls(req.getCapa())
                .vendedor(vendedor)
                .aprovado(true)
                .dataAprovacao(LocalDateTime.now())
                .adminAprovadorId(req.getAdminId())
                .emPromocao(promoAtiva)
                .promocaoExpira(promoAtiva ? req.getPromocaoExpira() : null)
                .build();

        Livro salvo = livroRepository.save(livro);

        return livroMapper.paraDTO(salvo);
    }

    @Transactional
    public LivroDTO editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        // Captura estado anterior de promoção antes de modificar
        boolean eraPromocao = Boolean.TRUE.equals(livro.getEmPromocao());

        // ===== dados básicos =====
        livro.setTitulo(req.getTitulo());
        livro.setAutor(req.getAutor());
        livro.setIsbn(req.getIsbn());
        livro.setEstadoAprovado(req.getEstado());
        livro.setResumoOficial(req.getResumo());
        livro.setFotosUrls(req.getCapa());

        // ===== regra de promoção =====
        boolean promoAtiva = Boolean.TRUE.equals(req.getEmPromocao())
                && req.getPercentualDesconto() != null
                && req.getPercentualDesconto() > 0;

        livro.setEmPromocao(promoAtiva);

        double precoPromo = 0.0;
        if (promoAtiva) {
            livro.setPrecoOriginal(req.getPreco());
            precoPromo = req.getPreco() * (1.0 - req.getPercentualDesconto() / 100.0);
            livro.setPrecoAprovado(precoPromo);
            livro.setPromocaoExpira(req.getPromocaoExpira());
        } else {
            livro.setPrecoAprovado(req.getPreco());
            livro.setPrecoOriginal(null);
            livro.setPromocaoExpira(null);
        }

        Livro salvo = livroRepository.save(livro);

        // Notifica usuários da lista de desejos quando promoção é ativada
        if (promoAtiva && !eraPromocao && livro.getIsbn() != null) {
            final double precoFinal = precoPromo;
            try {
                listaDesejosService.notificarClientesSeEmPromocao(
                        livro.getIsbn(), livro.getTitulo(), precoFinal);
            } catch (Exception e) {
                log.error("Erro ao notificar wishlist sobre promoção do livro {}: {}", id, e.getMessage());
            }
        }

        return livroMapper.paraDTO(salvo);
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroRepository.deleteById(id);
    }

    // ========================= CONVERSÃO =========================

    private List<LivroDTO> converterLista(List<Livro> livros) {
        List<LivroDTO> lista = new ArrayList<>();

        for (Livro livro : livros) {
            lista.add(livroMapper.paraDTO(livro));
        }

        return lista;
    }
}