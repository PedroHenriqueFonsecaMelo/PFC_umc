package umc.exs.controller.api;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import umc.exs.model.daos.repository.LivroRepository;
import umc.exs.model.dtos.LivroRequestDTO;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.service.LivroService;

@RestController
@RequestMapping("/api/livros")
@RequiredArgsConstructor
public class LivroControllerApi {

    private final LivroService livroService;
    private final LivroRepository livroRepository;

    @PostMapping(value = "/vender", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> criarAnuncio(
            @AuthenticationPrincipal UserDetails user,
            @RequestPart("dados") LivroRequestDTO dados,
            @RequestPart("foto") MultipartFile foto) {

        try {
            LivroAnuncio anuncio = livroService.cadastrarVenda(user.getUsername(), dados, foto);
            return ResponseEntity.ok(anuncio);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/todos")
    public List<LivroAnuncio> listarTodos() {
        return livroRepository.findAll(); // Variável corrigida
    }

    @PostMapping("/{id}/comprar")
    public ResponseEntity<?> comprarLivro(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        try {
            livroService.realizarCompra(id, user.getUsername());
            return ResponseEntity.ok("Compra realizada com sucesso! Tokens transferidos.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao processar a transação.");
        }
    }
}