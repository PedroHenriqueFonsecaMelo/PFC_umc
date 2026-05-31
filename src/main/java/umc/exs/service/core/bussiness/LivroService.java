package umc.exs.service.core.bussiness;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.request.admin.LivroAdminRequest;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;

@Service("livroService")
@RequiredArgsConstructor
public class LivroService {

    private final LivroCompraService livroCompraService;
    private final LivroAnuncioService livroAnuncioService;
    private final LivroAdminService livroAdminService;

    // ========================= COMPRA =========================

    @Transactional
    public void realizarCompra(@NonNull Long livroId, String email) {
        livroCompraService.realizarCompra(livroId, email);
    }

    @Transactional
    public CarrinhoCompraResponse comprarCarrinho(String email, CarrinhoCompraRequest request) {
        return livroCompraService.comprarCarrinho(email, request);
    }

    // ========================= ANÚNCIO =========================

    @Transactional
    public Livro cadastrarVenda(String email, LivroRequest dto, MultipartFile foto) {
        return livroAnuncioService.cadastrarVenda(email, dto, foto);
    }

    @Transactional
    public Lote criarLote(String email, LoteRequest dto, List<MultipartFile> fotos) {
        return livroAnuncioService.criarLote(email, dto, fotos);
    }

    @Transactional
    public List<Livro> listarPromocoesAtivas() {
        return livroAnuncioService.listarPromocoesAtivas();
    }

    // ========================= ADMIN =========================

    @Transactional
    public List<Livro> listarLivrosPendentes() {
        return livroAdminService.listarLivrosPendentes();
    }

    @Transactional
    public List<Livro> listarLivrosAprovados() {
        return livroAdminService.listarLivrosAprovados();
    }

    public Page<Livro> listarLivrosAprovadosPaginado(Pageable pageable) {
        return livroAdminService.listarLivrosAprovadosPaginado(pageable);
    }

    public Page<Livro> listarPromocoesAtivasPaginado(Pageable pageable) {
        return livroAdminService.listarPromocoesAtivasPaginado(pageable);
    }

    @Transactional
    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroAdminService.listarLivrosPorLote(loteId);
    }

    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, umc.exs.dto.request.admin.AdminAprovacaoRequest dto) {
        return livroAdminService.aprovarLivro(livroId, adminId, dto);
    }

    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {
        livroAdminService.rejeitarLivro(livroId, adminId, estado, comentario);
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroAdminService.deletarLivroAdmin(id);
    }

    public String salvarFotoLivro(MultipartFile foto) {
        return livroAdminService.salvarFotoLivro(foto);
    }

    @Transactional
    public Livro adicionarLivroAdmin(LivroAdminRequest req) {

        return livroAdminService.adicionarLivroAdmin(req);
    }

    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        return livroAdminService.editarLivroAdmin(id, req);
    }

    @Transactional
    public Livro buscarPorIdAtivo(Long id) {
        return livroAnuncioService.buscarPorIdAtivo(id);
    }

    @Transactional
    public Livro cadastrarPorIsbn(String isbn) {
        return livroAnuncioService.cadastrarPorIsbn(isbn);
    }
}