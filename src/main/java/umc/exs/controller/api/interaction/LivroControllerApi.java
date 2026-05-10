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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import umc.exs.repository.livro.LivroRepository;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraRequestDTO;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraResponseDTO;
import umc.exs.dtos.compra.lote.LoteRequestDTO;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.dtos.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.service.core.bussiness.LivroService;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroControllerApi {

    private final LivroService livroService;
    
    // Constante para resolver java:S1192
    private static final String MSG_USUARIO_NAO_LOGADO = "Usuário precisa estar logado.";

    @PostMapping(value = "/lotes/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> criarLoteVenda(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("loteDados") LoteRequestDTO loteDados,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {

        if (user == null) {
            return ResponseEntity.status(401).body(MSG_USUARIO_NAO_LOGADO);
        }

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

    @PostMapping(value = "/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Object> criarAnuncio(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("dados") LivroRequestDTO dados,
            @RequestPart("foto") MultipartFile foto) {

        if (user == null) {
            return ResponseEntity.status(401).body(MSG_USUARIO_NAO_LOGADO);
        }

        try {
            LivroDTO anuncio = livroService.cadastrarVenda(user.getUsername(), dados, foto);
            return ResponseEntity.ok(anuncio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao criar anúncio: " + e.getMessage());
        }
    }

    @GetMapping("/todos")
    public ResponseEntity<List<LivroDTO>> listarTodos(@RequestParam(required = false) Boolean emPromocao) {

        if (Boolean.TRUE.equals(emPromocao)) {
            return ResponseEntity.ok(
                    livroService.listarPromocoesAtivas());
        }

        return ResponseEntity.ok(
                livroService.listarLivrosAprovados());
    }

    @PostMapping("/{id}/comprar")
    public ResponseEntity<Object> comprarLivro(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).body(MSG_USUARIO_NAO_LOGADO);
        }

        try {
            livroService.realizarCompra(id, user.getUsername());
            return ResponseEntity.ok("Compra realizada com sucesso! Tokens transferidos.");
        } catch (IllegalStateException e) { // Usando exceção específica tratada no Service
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar a transação.");
        }
    }

    @PostMapping("/carrinho/comprar")
    public ResponseEntity<Object> comprarCarrinho(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody CarrinhoCompraRequestDTO request) {

        if (user == null) {
            return ResponseEntity.status(401).body(MSG_USUARIO_NAO_LOGADO);
        }

        try {
            CarrinhoCompraResponseDTO resultado = livroService.comprarCarrinho(user.getUsername(), request);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao processar o carrinho: " + e.getMessage());
        }
    }
}