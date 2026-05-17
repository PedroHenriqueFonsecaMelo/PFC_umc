package umc.exs.service.core.bussiness;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.admin.LivroAdminRequest;
import umc.exs.dto.compra.carrinho.CarrinhoCompraRequestDTO;
import umc.exs.dto.compra.carrinho.CarrinhoCompraResponseDTO;
import umc.exs.dto.compra.lote.LoteRequestDTO;
import umc.exs.dto.livro.LivroDTO;
import umc.exs.dto.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;

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
    public CarrinhoCompraResponseDTO comprarCarrinho(String email, CarrinhoCompraRequestDTO request) {
        return livroCompraService.comprarCarrinho(email, request);
    }

    // ========================= ANÚNCIO =========================

    @Transactional
    public LivroDTO cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
        return livroAnuncioService.cadastrarVenda(email, dto, foto);
    }

    @Transactional
    public Lote criarLote(String email, LoteRequestDTO dto, List<MultipartFile> fotos) {
        return livroAnuncioService.criarLote(email, dto, fotos);
    }

    @Transactional
    public List<LivroDTO> listarPromocoesAtivas() {
        return livroAnuncioService.listarPromocoesAtivas();
    }

    // ========================= ADMIN =========================

    @Transactional
    public List<LivroDTO> listarLivrosPendentes() {
        return livroAdminService.listarLivrosPendentes();
    }

    @Transactional
    public List<LivroDTO> listarLivrosAprovados() {
        return livroAdminService.listarLivrosAprovados();
    }

    public Page<LivroDTO> listarLivrosAprovadosPaginado(Pageable pageable) {
        return livroAdminService.listarLivrosAprovadosPaginado(pageable);
    }

    public Page<LivroDTO> listarPromocoesAtivasPaginado(Pageable pageable) {
        return livroAdminService.listarPromocoesAtivasPaginado(pageable);
    }

    @Transactional
    public List<LivroDTO> listarLivrosPorLote(Long loteId) {
        return livroAdminService.listarLivrosPorLote(loteId);
    }

    @Transactional
    public LivroDTO aprovarLivro(Long livroId, Long adminId, umc.exs.dto.admin.AdminAprovacaoDTO dto) {
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

    @Transactional
    public LivroDTO adicionarLivroAdmin(LivroAdminRequest req) {

        return livroAdminService.adicionarLivroAdmin(req);
    }

    @Transactional
    public LivroDTO editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        return livroAdminService.editarLivroAdmin(id, req);
    }

    @Transactional
    public LivroDTO buscarPorIdAtivo(Long id) {
        return livroAnuncioService.buscarPorIdAtivo(id);
    }

    @Transactional
    public LivroDTO cadastrarPorIsbn(String isbn) {
        return livroAnuncioService.cadastrarPorIsbn(isbn);
    }
}