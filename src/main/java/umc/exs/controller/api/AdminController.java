package umc.exs.controller.api;

import java.util.List;
import java.util.Optional;

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
import umc.exs.model.daos.repository.AdminRepository;
import umc.exs.model.dtos.AdminAprovacaoDTO;
import umc.exs.model.entidades.foundation.Administrador;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.service.LivroService;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final LivroService livroService;
    private final AdminRepository adminRepository;

    @GetMapping("/livros/pendentes")
    public ResponseEntity<List<LivroAnuncio>> listarLivrosPendentes() {
        // O Service já retorna a lista filtrada usando o loop tradicional
        List<LivroAnuncio> livros = livroService.listarLivrosPendentes();
        return ResponseEntity.ok(livros);
    }

    @PostMapping("/livros/{id}/aprovar")
    public ResponseEntity<?> aprobarLivro(
            @PathVariable Long id,
            @RequestBody AdminAprovacaoDTO aprovacao,
            @AuthenticationPrincipal UserDetails user) {
        
        if (user == null) {
            return ResponseEntity.status(401).body("Acesso negado: Admin não autenticado.");
        }

        try {
            // Busca tradicional do Admin
            Optional<Administrador> adminOpt = adminRepository.findByEmail(user.getUsername());
            
            if (adminOpt.isEmpty()) {
                throw new RuntimeException("A conta de administrador não foi localizada no banco de dados.");
            }
            
            Long adminId = adminOpt.get().getId();
            
            // Aqui o service executa a lógica onde o Admin define o preço e estado
            LivroAnuncio livro = livroService.aprovarLivro(id, adminId, aprovacao);
            return ResponseEntity.ok(livro);
            
        } catch (RuntimeException e) {
            // Captura qualquer erro de validação (como preço abusivo) e retorna para o front
            return ResponseEntity.badRequest().body("Erro na aprovação: " + e.getMessage());
        }
    }

    @PostMapping("/livros/{id}/rejeitar")
    public ResponseEntity<?> rejeitarLivro(
            @PathVariable Long id,
            @RequestBody RejeicaoDTO rejeicao,
            @AuthenticationPrincipal UserDetails user) {
        
        if (user == null) {
            return ResponseEntity.status(401).body("Acesso negado: Admin não autenticado.");
        }

        try {
            Optional<Administrador> adminOpt = adminRepository.findByEmail(user.getUsername());
            
            if (adminOpt.isEmpty()) {
                throw new RuntimeException("Admin não encontrado.");
            }
            
            Long adminId = adminOpt.get().getId();
            
            livroService.rejeitarLivro(id, adminId, rejeicao.getComentario());
            return ResponseEntity.ok("O livro foi rejeitado e removido da fila de análise.");
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erro ao rejeitar: " + e.getMessage());
        }
    }

    // DTO estática para garantir que o Jackson consiga instanciar sem problemas
    @lombok.Data
    public static class RejeicaoDTO {
        private String comentario;
    }
}