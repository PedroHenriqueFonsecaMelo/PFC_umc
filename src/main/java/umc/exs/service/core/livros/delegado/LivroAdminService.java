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

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAdminService {

    private final LivroRepository livroRepository;

    // serviços de responsabilidade separada
    private final LivroAprovacaoService livroAprovacaoService;
    private final LivroPromocaoService livroPromocaoService;

    // dependências restantes para CRUD admin
    private final ClienteRepository clienteRepository;
    private final ExternApi googleBooksService;

    private final ListaDesejosService listaDesejosService;

    private static final String URL_UPLOAD = "uploads/livros/";

    // ========================= LISTAGENS =========================

    public List<Livro> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    public List<Livro> listarLivrosAprovados() {
        return livroRepository.findByAprovadoTrue();
    }

    public Page<Livro> listarLivrosAprovadosPaginado(Pageable pageable) {
        return livroRepository.findByAprovadoTrue(pageable);
    }

    public Page<Livro> listarPromocoesAtivasPaginado(Pageable pageable) {
        return livroRepository.findPromocoesAtivasPaginado(LocalDateTime.now(), pageable);
    }

    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroRepository.findByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(loteId);
    }

    // ========================= APROVAÇÃO =========================

    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, umc.exs.dto.request.admin.AdminAprovacaoRequest dto) {
        return livroAprovacaoService.aprovarLivro(livroId, adminId, dto);
    }

    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {
        livroAprovacaoService.rejeitarLivro(livroId, adminId, estado, comentario);
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

        // criação do livro base (preços/vigência via promo service)
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

        // aplica promoção
        livroPromocaoService.aplicarPromocao(
                livro,
                promoAtiva,
                req.getPreco(),
                req.getPercentualDesconto(),
                promoAtiva ? req.getPromocaoExpira() : null);

        // gênero
        if (req.getGenero() != null && !req.getGenero().isBlank()) {
            livro.setGenero(req.getGenero());
        } else if (livro.getIsbn() != null) {
            try {
                String genero = googleBooksService.buscarGeneroPorIsbn(livro.getIsbn());
                if (genero != null) {
                    livro.setGenero(genero);
                }
            } catch (Exception e) {
                log.warn("Não foi possível buscar gênero: {}", e.getMessage());
            }
        }

        return livroRepository.save(livro);
    }

    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        boolean eraPromocao = Boolean.TRUE.equals(livro.getEmPromocao());

        // dados básicos
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

        // Notifica usuários da lista de desejos quando promoção é ativada
        if (promoAtiva && !eraPromocao && livro.getIsbn() != null) {
            final double precoFinal = livro.getPrecoAprovado() != null ? livro.getPrecoAprovado() : 0.0;
            try {
                listaDesejosService.notificarClientesSeEmPromocao(
                        livro.getIsbn(),
                        livro.getTitulo(),
                        precoFinal);
            } catch (Exception e) {
                log.error("Erro ao notificar wishlist sobre promoção do livro {}: {}", id, e.getMessage());
            }
        }

        return salvo;
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroRepository.deleteById(id);
    }
}
