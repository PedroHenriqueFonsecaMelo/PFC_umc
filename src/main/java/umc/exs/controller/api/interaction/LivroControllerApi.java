package umc.exs.controller.api.interaction;

import java.util.List;

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

import umc.exs.repository.livro.LivroRepository;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraRequestDTO;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraResponseDTO;
import umc.exs.dtos.compra.lote.LoteRequestDTO;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.dtos.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.service.core.bussiness.LivroService;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroControllerApi {

    private final LivroService livroService;

    private static final String MSG_USUARIO_NAO_LOGADO = "Usuário precisa estar logado.";

    /**
     * Lista livros aprovados para a vitrine pública.
     * Suporta filtro opcional para promoções.
     */
    @GetMapping("/todos")
    public ResponseEntity<List<LivroDTO>> listarTodos(@RequestParam(required = false) Boolean emPromocao) {
        if (Boolean.TRUE.equals(emPromocao)) {
            return ResponseEntity.ok(livroService.listarPromocoesAtivas());
        }
        return ResponseEntity.ok(livroService.listarLivrosAprovados());
    }

    /**
     * Busca um livro específico por ID (Endpoint Público).
     */
    @GetMapping("/{id}")
    public ResponseEntity<LivroDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(livroService.buscarPorIdAtivo(id));
    }

    /**
     * Cadastro de lote com múltiplos livros e fotos.
     */
    @PostMapping(value = "/lotes/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> criarLoteVenda(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("loteDados") LoteRequestDTO loteDados,
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
            @RequestPart("dados") LivroRequestDTO dados,
            @RequestPart("foto") MultipartFile foto) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            LivroDTO anuncio = livroService.cadastrarVenda(user.getUsername(), dados, foto);
            return ResponseEntity.ok(anuncio);
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
            @Valid @RequestBody CarrinhoCompraRequestDTO request) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(MSG_USUARIO_NAO_LOGADO);

        try {
            CarrinhoCompraResponseDTO resultado = livroService.comprarCarrinho(user.getUsername(), request);
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
            LivroDTO livro = livroService.cadastrarPorIsbn(isbn);
            return ResponseEntity.ok(livro);
        } catch (Exception e) {
            // Se o Google cair (503), retornamos um erro limpo para o JS
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Serviço do Google temporariamente fora do ar.");
        }
    }
}