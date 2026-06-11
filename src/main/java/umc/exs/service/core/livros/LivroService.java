package umc.exs.service.core.livros;

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
import umc.exs.service.core.livros.delegado.LivroAdminService;
import umc.exs.service.core.livros.delegado.LivroAnuncioService;
import umc.exs.service.core.livros.delegado.LivroCompraService;

/**
 * Fachada principal do domínio de livros, delegando operações para serviços especializados.
 * Concentra compra, anúncio, administração e atualização de preços em um único ponto de entrada.
 */
@Service("livroService")
@RequiredArgsConstructor
public class LivroService {

    private final LivroCompraService livroCompraService;
    private final LivroAnuncioService livroAnuncioService;
    private final LivroAdminService livroAdminService;

    // ========================= COMPRA =========================

    /**
     * Realiza a compra de um livro individual, debitando tokens e registrando o pedido.
     */
    @Transactional
    public void realizarCompra(@NonNull Long livroId, String email) {
        livroCompraService.realizarCompra(livroId, email);
    }

    /**
     * Processa a compra de múltiplos livros de um carrinho em uma única transação.
     */
    @Transactional
    public CarrinhoCompraResponse comprarCarrinho(String email, CarrinhoCompraRequest request) {
        return livroCompraService.comprarCarrinho(email, request);
    }

    // ========================= ANÚNCIO =========================

    /**
     * Cadastra um anúncio de venda individual com foto e validação via ISBN na API externa.
     */
    @Transactional
    public Livro cadastrarVenda(String email, LivroRequest dto, MultipartFile foto) {
        return livroAnuncioService.cadastrarVenda(email, dto, foto);
    }

    /**
     * Cria um lote de livros para venda, associando múltiplas fotos aos itens enviados.
     */
    @Transactional
    public Lote criarLote(String email, LoteRequest dto, List<MultipartFile> fotos) {
        return livroAnuncioService.criarLote(email, dto, fotos);
    }

    /**
     * Retorna a lista de livros com promoção ativa no momento da consulta.
     */
    @Transactional
    public List<Livro> listarPromocoesAtivas() {
        return livroAnuncioService.listarPromocoesAtivas();
    }

    // ========================= ADMIN =========================

    /**
     * Lista os livros aguardando análise e aprovação pelo administrador.
     */
    @Transactional
    public List<Livro> listarLivrosPendentes() {
        return livroAdminService.listarLivrosPendentes();
    }

    /**
     * Lista todos os livros já aprovados e disponíveis para venda.
     */
    @Transactional
    public List<Livro> listarLivrosAprovados() {
        return livroAdminService.listarLivrosAprovados();
    }

    /**
     * Retorna os livros aprovados de forma paginada, com suporte a busca por texto.
     */
    public Page<Livro> listarLivrosAprovadosPaginado(Pageable pageable, String busca) {
        return livroAdminService.listarLivrosAprovadosPaginado(pageable, busca);
    }

    /**
     * Retorna as promoções ativas de forma paginada, com suporte a busca por texto.
     */
    public Page<Livro> listarPromocoesAtivasPaginado(Pageable pageable, String busca) {
        return livroAdminService.listarPromocoesAtivasPaginado(pageable, busca);
    }

    /**
     * Lista todos os livros pertencentes a um lote específico.
     */
    @Transactional
    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroAdminService.listarLivrosPorLote(loteId);
    }

    /**
     * Aprova um livro pendente, definindo preço e recompensando o vendedor.
     */
    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, umc.exs.dto.request.admin.AdminAprovacaoRequest dto) {
        return livroAdminService.aprovarLivro(livroId, adminId, dto);
    }

    /**
     * Rejeita um livro pendente, registrando o motivo e notificando o vendedor.
     */
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {
        livroAdminService.rejeitarLivro(livroId, adminId, estado, comentario);
    }

    /**
     * Remove permanentemente um livro pelo administrador.
     */
    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroAdminService.deletarLivroAdmin(id);
    }

    /**
     * Cadastra um novo livro diretamente pelo painel administrativo.
     */
    @Transactional
    public Livro adicionarLivroAdmin(LivroAdminRequest req) {

        return livroAdminService.adicionarLivroAdmin(req);
    }

    /**
     * Edita os dados de um livro existente pelo painel administrativo.
     */
    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, LivroAdminRequest req) {

        return livroAdminService.editarLivroAdmin(id, req);
    }

    /**
     * Busca um livro aprovado pelo ID, lançando exceção se estiver inativo ou inexistente.
     */
    @Transactional
    public Livro buscarPorIdAtivo(Long id) {
        return livroAnuncioService.buscarPorIdAtivo(id);
    }

    /**
     * Cadastra um livro automaticamente consultando a API do Google Books pelo ISBN.
     */
    @Transactional
    public Livro cadastrarPorIsbn(String isbn) {
        return livroAnuncioService.cadastrarPorIsbn(isbn);
    }

    // ========================= ADMIN / ATUALIZAÇÕES =========================

    /**
     * Aplica a taxa de inflação IPCA acumulada sobre o preço aprovado de um livro.
     */
    @Transactional
    public Livro aplicarInflacaoIpcaNoPrecoAprovado(@NonNull Long livroId, Double taxaIpcaAcumulado) {
        return livroAdminService.aplicarInflacaoIpcaNoPrecoAprovado(livroId, taxaIpcaAcumulado);
    }


}