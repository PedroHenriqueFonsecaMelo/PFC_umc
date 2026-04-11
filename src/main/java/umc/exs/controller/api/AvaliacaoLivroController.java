package umc.exs.controller.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.livro.AvaliacaoLivroDTO;
import umc.exs.model.entidades.foundation.AvaliacaoLivro;
import umc.exs.service.core.AvaliacaoLivroService;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoLivroController {

    private final AvaliacaoLivroService avaliacaoService;

    @PostMapping
    public ResponseEntity<?> criarAvaliacao(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody AvaliacaoLivroDTO dto) {

        if (user == null) {
            return ResponseEntity.status(401).body("Usuário precisa estar logado.");
        }

        try {
            AvaliacaoLivro avaliacao = avaliacaoService.criarAvaliacao(user.getUsername(), dto);
            return ResponseEntity.ok(avaliacao);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao criar avaliação.");
        }  // ← brace corrigida
    }

    @GetMapping("/livro/{isbn}")
    public ResponseEntity<?> buscarAvaliacoes(@PathVariable String isbn) {
        try {
            List<AvaliacaoLivro> avaliacoes = avaliacaoService.buscarAvaliacoesPorIsbn(isbn);
            return ResponseEntity.ok(avaliacoes);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/livro/{isbn}/media")
    public ResponseEntity<?> buscarMedia(@PathVariable String isbn) {
        try {
            Double media = avaliacaoService.calcularMediaPorIsbn(isbn);
            if (media == null) {
                return ResponseEntity.ok("{\"media\": null, \"mensagem\": \"Nenhuma avaliação ainda\"}");
            }
            return ResponseEntity.ok("{\"media\": " + media + "}");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
