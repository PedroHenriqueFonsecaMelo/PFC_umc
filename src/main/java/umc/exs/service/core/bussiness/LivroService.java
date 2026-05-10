package umc.exs.service.core.bussiness;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraRequestDTO;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraResponseDTO;
import umc.exs.dtos.compra.lote.LoteRequestDTO;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.dtos.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.enums.EstadoLivro;

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

    public List<LivroDTO> listarLivrosPendentes() {
        return livroAdminService.listarLivrosPendentes();
    }

    public List<LivroDTO> listarLivrosAprovados() {
        return livroAdminService.listarLivrosAprovados();
    }

    public List<LivroDTO> listarLivrosPorLote(Long loteId) {
        return livroAdminService.listarLivrosPorLote(loteId);
    }

    @Transactional
    public LivroDTO aprovarLivro(Long livroId, Long adminId, umc.exs.dtos.admin.AdminAprovacaoDTO dto) {
        return livroAdminService.aprovarLivro(livroId, adminId, dto);
    }

    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {
        livroAdminService.rejeitarLivro(livroId, adminId, estado, comentario);
    }

    @Transactional
    public LivroDTO adicionarLivroAdmin(String titulo, String autor, String isbn,
            Double preco, EstadoLivro estado,
            String capa, Long vendedorId) {
        return livroAdminService.adicionarLivroAdmin(
                titulo, autor, isbn, preco, estado, capa, vendedorId);
    }

    @Transactional
    public LivroDTO editarLivroAdmin(Long id, String titulo, String autor,
            String isbn, Double preco,
            EstadoLivro estado, String capa) {
        return livroAdminService.editarLivroAdmin(
                id, titulo, autor, isbn, preco, estado, capa);
    }

    @Transactional
    public void deletarLivroAdmin(Long id) {
        livroAdminService.deletarLivroAdmin(id);
    }

    @Transactional
    public LivroDTO cadastrarPorIsbn(String isbn) {
        return livroCompraService.cadastrarPorIsbn(isbn);
    }
}