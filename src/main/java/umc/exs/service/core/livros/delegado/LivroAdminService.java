package umc.exs.service.core.livros.delegado;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.dto.request.admin.LivroAdminRequest;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.api.ExternApi;
import umc.exs.service.core.dashboard.ListaDesejosService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAdminService {

    private final LivroRepository livroRepository;

    private final LivroAprovacaoService livroAprovacaoService;
    private final LivroPromocaoService livroPromocaoService;

    private final ClienteRepository clienteRepository;
    private final ExternApi googleBooksService;

    private final ListaDesejosService listaDesejosService;
    private final LogAuditoriaService logAuditoria;

    private static final String LOG_LIVRO_ADMIN_CRIADO = "LIVRO_ADMIN_CRIADO";
    private static final String LOG_LIVRO_ADMIN_EDITADO = "LIVRO_ADMIN_EDITADO";
    private static final String LOG_LIVRO_ADMIN_REMOVIDO = "LIVRO_ADMIN_REMOVIDO";

    private static final String LOG_LIVROS_PENDENTES_LISTADOS = "LIVROS_PENDENTES_LISTADOS";
    private static final String LOG_LIVROS_APROVADOS_LISTADOS = "LIVROS_APROVADOS_LISTADOS";
    private static final String LOG_PROMOCOES_ATIVAS_LISTADAS = "PROMOCOES_ATIVAS_LISTADAS";
    private static final String LOG_LIVROS_POR_LOTE_LISTADOS = "LIVROS_POR_LOTE_LISTADOS";

    private static final String LOG_LIVRO_APROVADO = "LIVRO_APROVADO";
    private static final String LOG_LIVRO_REJEITADO = "LIVRO_REJEITADO";

    // ========================= LISTAGENS =========================

    public List<Livro> listarLivrosPendentes() {

        List<Livro> livros = livroRepository.findByAprovadoFalse();

        logAuditoria.registrarLog(
                LOG_LIVROS_PENDENTES_LISTADOS,
                null,
                null,
                "total=" + livros.size()
        );

        return livros;
    }

    public List<Livro> listarLivrosAprovados() {

        List<Livro> livros = livroRepository.findByAprovadoTrue();

        logAuditoria.registrarLog(
                LOG_LIVROS_APROVADOS_LISTADOS,
                null,
                null,
                "total=" + livros.size()
        );

        return livros;
    }

    public Page<Livro> listarLivrosAprovadosPaginado(Pageable pageable, String busca) {

        if (busca == null || busca.isBlank()) {
            Page<Livro> page = livroRepository.findByAprovadoTrue(pageable);
            logAuditoria.registrarLog(
                    LOG_LIVROS_APROVADOS_LISTADOS,
                    null,
                    null,
                    "page=" + pageable.getPageNumber() + ", size=" + pageable.getPageSize()
            );
            return page;
        }

        // Para busca com acentos no SQLite, filtrar em memória
        String termo = normalize(busca.trim());

        List<Livro> todos = livroRepository.findByAprovadoTrue();
        List<Livro> filtrados = todos.stream()
                .filter(l -> {
                    String titulo = normalize(l.getTitulo());
                    String autor = normalize(l.getAutor());
                    String isbn = l.getIsbn() != null ? l.getIsbn().toLowerCase() : "";
                    return titulo.contains(termo) || autor.contains(termo) || isbn.contains(termo);
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtrados.size());
        List<Livro> pagina = start >= filtrados.size()
                ? java.util.Collections.emptyList()
                : filtrados.subList(start, end);

        logAuditoria.registrarLog(
                LOG_LIVROS_APROVADOS_LISTADOS,
                null,
                null,
                "busca=" + busca + ", page=" + pageable.getPageNumber() + ", total=" + filtrados.size()
        );

        return new org.springframework.data.domain.PageImpl<>(pagina, pageable, filtrados.size());
    }

    public Page<Livro> listarPromocoesAtivasPaginado(Pageable pageable, String busca) {

        Page<Livro> page = (busca != null && !busca.isBlank())
                ? livroRepository.findPromocoesAtivasPaginadoComBusca(LocalDateTime.now(), normalizarBusca(busca), normalizarBuscaSemAcento(busca), pageable)
                : livroRepository.findPromocoesAtivasPaginado(LocalDateTime.now(), pageable);

        logAuditoria.registrarLog(
                LOG_PROMOCOES_ATIVAS_LISTADAS,
                null,
                null,
                "page=" + pageable.getPageNumber()
        );

        return page;
    }

    public List<Livro> listarLivrosPorLote(Long loteId) {

        List<Livro> livros = livroRepository
                .findByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(loteId);

        logAuditoria.registrarLog(
                LOG_LIVROS_POR_LOTE_LISTADOS,
                null,
                null,
                "loteId=" + loteId + ", total=" + livros.size()
        );

        return livros;
    }

    // ========================= APROVAÇÃO =========================

    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId,
                              umc.exs.dto.request.admin.AdminAprovacaoRequest dto) {

        Livro livro = livroAprovacaoService.aprovarLivro(livroId, adminId, dto);

        logAuditoria.registrarLog(
                LOG_LIVRO_APROVADO,
                adminId,
                null,
                "livroId=" + livroId
        );

        return livro;
    }

    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        livroAprovacaoService.rejeitarLivro(livroId, adminId, estado, comentario);

        logAuditoria.registrarLog(
                LOG_LIVRO_REJEITADO,
                adminId,
                null,
                "livroId=" + livroId + ", estado=" + estado
        );
    }

    // ========================= CRUD ADMIN =========================

    @Transactional
    public Livro adicionarLivroAdmin(LivroAdminRequest req) {

        umc.exs.model.entidades.usuario.Cliente vendedor = null;
        if (req.getVendedorId() != null) {
            vendedor = clienteRepository.findById(req.getVendedorId())
                    .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));
        }

        boolean promoAtiva = Boolean.TRUE.equals(req.getEmPromocao())
                && req.getPercentualDesconto() != null
                && req.getPercentualDesconto() > 0;

        Livro livro = Livro.builder()
                .titulo(req.getTitulo())
                .autor(req.getAutor())
                .isbn(req.getIsbn())
                .estadoAprovado(req.getEstado())
                .resumoOficial(req.getResumo())
                .fotosUrls(req.getCapa())
                .vendedor(vendedor)
                .aprovado(true)
                .dataAprovacao(LocalDateTime.now())
                .adminAprovadorId(req.getAdminId())
                .emPromocao(promoAtiva)
                .build();

        livroPromocaoService.aplicarPromocao(
                livro,
                promoAtiva,
                req.getPreco(),
                req.getPercentualDesconto(),
                promoAtiva ? req.getPromocaoExpira() : null);

        if (req.getGenero() != null && !req.getGenero().isBlank()) {
            livro.setGenero(req.getGenero());
        } else if (livro.getIsbn() != null) {
            try {
                String genero = googleBooksService.buscarGeneroPorIsbn(livro.getIsbn());
                livro.setGenero(genero);
            } catch (Exception e) {
                log.warn("GENERO_API_FALHA isbn={} erro={}", livro.getIsbn(), e.getMessage());
            }
        }

        Livro salvo = livroRepository.save(livro);

        logAuditoria.registrarLog(
                LOG_LIVRO_ADMIN_CRIADO,
                req.getAdminId(),
                null,
                "isbn=" + req.getIsbn() + ", titulo=" + req.getTitulo()
        );

        return salvo;
    }

    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        boolean eraPromocao = Boolean.TRUE.equals(livro.getEmPromocao());

        livro.setTitulo(req.getTitulo());
        livro.setAutor(req.getAutor());
        livro.setIsbn(req.getIsbn());
        livro.setEstadoAprovado(req.getEstado());
        livro.setResumoOficial(req.getResumo());

        if (req.getCapa() != null && !req.getCapa().isBlank()) {
            livro.setFotosUrls(req.getCapa());
        }

        boolean promoAtiva = Boolean.TRUE.equals(req.getEmPromocao())
                && req.getPercentualDesconto() != null
                && req.getPercentualDesconto() > 0;

        livro.setEmPromocao(promoAtiva);

        livroPromocaoService.aplicarPromocao(
                livro,
                promoAtiva,
                req.getPreco(),
                req.getPercentualDesconto(),
                promoAtiva ? req.getPromocaoExpira() : null);

        if (req.getGenero() != null) {
            livro.setGenero(req.getGenero().isBlank() ? null : req.getGenero());
        }

        Livro salvo = livroRepository.save(livro);

        if (promoAtiva && !eraPromocao && livro.getIsbn() != null) {
            try {
                listaDesejosService.notificarClientesSeEmPromocao(
                        livro.getIsbn(),
                        livro.getTitulo(),
                        livro.getPrecoAprovado() != null ? livro.getPrecoAprovado() : 0.0
                );
            } catch (Exception e) {
                log.error("WISHLIST_NOTIFY_FAIL livroId={} erro={}", id, e.getMessage());
            }
        }

        logAuditoria.registrarLog(
                LOG_LIVRO_ADMIN_EDITADO,
                req.getAdminId(),
                null,
                "livroId=" + id + ", isbn=" + req.getIsbn()
        );

        return salvo;
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {

        livroRepository.deleteById(id);

        logAuditoria.registrarLog(
                LOG_LIVRO_ADMIN_REMOVIDO,
                null,
                null,
                "livroId=" + id
        );
    }

    @Transactional
    public Livro aplicarInflacaoIpcaNoPrecoAprovado(Long livroId, Double taxaIpcaAcumulado) {
        // 1. Busca o livro usando o repositório injetado na classe especialista
        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro com ID " + livroId + " não encontrado."));

        if (livro.getPrecoAprovado() == null) {
            throw new IllegalStateException("O livro informado não possui um preço aprovado cadastrado.");
        }

        // 2. Transfere o preço antigo para precoOriginal (mantendo o histórico)
        livro.setPrecoOriginal(livro.getPrecoAprovado());

        // 3. Calcula o novo preço com base na taxa percentual (ex: 4.5 para 4.5%)
        Double novoPreco = livro.getPrecoAprovado() * (1 + (taxaIpcaAcumulado / 100));

        // 4. Arredonda matematicamente para 2 casas decimais
        novoPreco = Math.round(novoPreco * 100.0) / 100.0;

        // 5. Atualiza o valor final
        livro.setPrecoAprovado(novoPreco);

        // 6. Retorna a entidade modificada (o Spring cuidará do commit devido ao @Transactional)
        return livroRepository.save(livro);
    }

    // ========================= UTILITÁRIOS =========================

    private String normalizarBusca(String busca) {
        if (busca == null) return null;
        return busca.trim();
    }

    private String normalizarBuscaSemAcento(String busca) {
        if (busca == null) return null;
        return java.text.Normalizer
                .normalize(busca.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer
                .normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase();
    }
}