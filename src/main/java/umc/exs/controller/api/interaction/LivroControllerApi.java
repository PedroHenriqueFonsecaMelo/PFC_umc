package umc.exs.controller.api.interaction;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.dto.response.compras.LivroExibicaoResponse;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.service.core.bussiness.LivroService;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroControllerApi {

    private final LivroService livroService;
    private final LivroMapper livroMapper;

    private static final String MSG_USUARIO_NAO_LOGADO = "Usuário precisa estar logado.";

    /**
     * Lista livros aprovados para a vitrine pública.
     * Suporta filtro opcional para promoções.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<LivroExibicaoResponse>> listarTodos(@RequestParam(required = false) Boolean emPromocao) {
        if (Boolean.TRUE.equals(emPromocao)) {
            return ResponseEntity.ok(livroService.listarPromocoesAtivas().stream()
                    .map(livroMapper::toResponse)
                    .toList());
        }
        return ResponseEntity.ok(livroService.listarLivrosAprovados().stream()
                .map(livroMapper::toResponse)
                .toList());
    }

    /**
     * Vitrine paginada — 20 livros por página.
     * Parâmetros: page (default 0), size (default 20), emPromocao (opcional).
     */
    @GetMapping("/vitrine")
    public ResponseEntity<Page<LivroExibicaoResponse>> listarVitrine(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    Boolean emPromocao) {

        var pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "id"));
        if (Boolean.TRUE.equals(emPromocao)) {
            return ResponseEntity.ok(livroService.listarPromocoesAtivasPaginado(pageable).map(livroMapper::toResponse));
        }
        return ResponseEntity.ok(livroService.listarLivrosAprovadosPaginado(pageable).map(livroMapper::toResponse));
    }

    /**
     * Busca um livro específico por ID (Endpoint Público).
     */
    @GetMapping("/{id}")
    public ResponseEntity<LivroExibicaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(livroMapper.toResponse(livroService.buscarPorIdAtivo(id)));
    }

    /**
     * Cadastro de lote com múltiplos livros e fotos.
     */
    @PostMapping(value = "/lotes/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> criarLoteVenda(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("loteDados") LoteRequest loteDados,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        if (fotos == null || fotos.isEmpty() || fotos.stream().allMatch(f -> f == null || f.isEmpty())) {
            return ResponseEntity.badRequest().body("É necessário adicionar pelo menos uma foto do livro.");
        }

        try {
            Lote lote = livroService.criarLote(user.getUsername(), loteDados, fotos);
            return ResponseEntity.ok(lote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar lote: " + e.getMessage());
        }
    }

    /**
     * Cadastro de anúncio individual.
     */
    @PostMapping(value = "/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> criarAnuncio(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("dados") LivroRequest dados,
            @RequestPart("foto") MultipartFile foto) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            return ResponseEntity.ok(livroMapper.toResponse(livroService.cadastrarVenda(user.getUsername(), dados, foto)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar anúncio: " + e.getMessage());
        }
    }

    /**
     * Compra de um único livro.
     */
    @PostMapping("/{id}/comprar")
    public ResponseEntity<Object> comprarLivro(
            @PathVariable @NonNull Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            livroService.realizarCompra(id, user.getUsername());
            return ResponseEntity.ok("Compra realizada com sucesso! Tokens transferidos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar a transação.");
        }
    }

    /**
     * Finalização de compra do carrinho completo.
     */
    @PostMapping("/carrinho/comprar")
    public ResponseEntity<Object> comprarCarrinho(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CarrinhoCompraRequest request) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            CarrinhoCompraResponse resultado = livroService.comprarCarrinho(user.getUsername(), request);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao processar o carrinho: " + e.getMessage());
        }
    }

    /**
     * Cadastro automático de livro via ISBN utilizando a API do Google Books.
     */
    @GetMapping("/cadastrar-isbn/{isbn}")
    public ResponseEntity<Object> cadastrarPorIsbn(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable String isbn) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            // Apenas busca e retorna os dados, sem salvar nada ainda
            return ResponseEntity.ok(livroMapper.toResponse(livroService.cadastrarPorIsbn(isbn)));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            // Google Books e OpenLibrary indisponíveis ou ISBN não encontrado em nenhuma delas
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Livro não encontrado automaticamente. Preencha os dados manualmente.");
        }
    }
}