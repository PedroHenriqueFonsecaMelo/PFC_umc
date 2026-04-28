package umc.exs.controller.api.interaction;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.compra.CarrinhoCompraRequestDTO;
import umc.exs.DTOs.compra.CarrinhoCompraResponseDTO;
import umc.exs.DTOs.compra.LoteRequestDTO;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.service.core.cliente.LivroService;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroControllerApi {

    private final LivroService livroService;

    /**
     * Cria lote venda múltiplos livros com fotos.
     * Multipart form LoteRequestDTO + List<MultipartFile>.
     * Chama LivroService.criarLote, retorna Lote.
     * Valida auth UserDetails.
     */
    @PostMapping(value = "/lotes/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarLoteVenda(

            @AuthenticationPrincipal UserDetails user,
            @RequestPart("loteDados") LoteRequestDTO loteDados,
            @RequestPart("fotos") List<MultipartFile> fotos) {

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário precisa estar logado.");
        }

        try {
            Lote lote = livroService.criarLote(user.getUsername(), loteDados, fotos);
            return ResponseEntity.ok(lote);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar lote: " + e.getMessage());
        }
    }

    /**
     * Cria anúncio único livro com foto.
     * Multipart LivroRequestDTO + MultipartFile foto.
     * Chama LivroService.cadastrarVenda, retorna LivroAnuncio.
     * Valida auth + try/catch erros.
     */
    @PostMapping(value = "/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarAnuncio(

            @AuthenticationPrincipal UserDetails user,
            // O Spring tentará converter o Blob JSON do React para este DTO
            @RequestPart("dados") LivroRequestDTO dados,
            @RequestPart("foto") MultipartFile foto) {

        // Validação básica de segurança
        if (user == null) {
            return ResponseEntity.status(401).body("Usuário precisa estar logado.");
        }

        try {
            Livro anuncio = livroService.cadastrarVenda(user.getUsername(), dados, foto);
            return ResponseEntity.ok(anuncio);
        } catch (Exception e) {
            // Logar o erro ajuda a debugar se o Service falhar
            return ResponseEntity.badRequest().body("Erro ao criar anúncio: " + e.getMessage());
        }
    }

    /**
     * Lista livros aprovados vitrine pública.
     * Chama LivroService.listarLivrosAprovados().
     * Sem auth, apenas aprovados.
     */
    @GetMapping("/todos")

    public ResponseEntity<List<Livro>> listarTodos() {
        // Retorna apenas livros aprovados para a vitrine
        return ResponseEntity.ok(livroService.listarLivrosAprovados());
    }

    /**
     * Compra livro aprovado por ID.
     * Valida saldo tokens, chama LivroService.realizarCompra.
     * Deduz tokens, remove vitrine, log sucesso.
     * Requer auth.
     */
    @PostMapping("/{id}/comprar")
    public ResponseEntity<?> comprarLivro(

            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário precisa estar logado para comprar.");
        }

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
     * Compra todos os livros do carrinho em uma única transação.
     * Valida saldo total antes de debitar qualquer valor.
     * Retorna resumo com comprados, falhas e saldo restante.
     * Requer autenticação.
     */
    @PostMapping("/carrinho/comprar")
    public ResponseEntity<?> comprarCarrinho(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CarrinhoCompraRequestDTO request) {

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário precisa estar logado para comprar.");
        }

        try {
            CarrinhoCompraResponseDTO resultado = livroService.comprarCarrinho(user.getUsername(), request);
            return ResponseEntity.ok(resultado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar o carrinho.");
        }
    }
}