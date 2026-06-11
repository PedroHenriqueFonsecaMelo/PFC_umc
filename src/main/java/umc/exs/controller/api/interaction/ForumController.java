package umc.exs.controller.api.interaction;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.ForumService;

/**
 * Controller REST do fórum de discussão da plataforma.
 * Permite listar tópicos paginados, curtir respostas, marcar melhor resposta e remover conteúdo (admin ou autor).
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

        private final ForumService forumService;
        private final ClienteRepository clienteRepo;

        private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        // ── GET /api/forum/topicos — Lista paginada (busca opcional) ──────────────

        /**
         * Lista os tópicos do fórum de forma paginada, com filtros opcionais de busca e categoria.
         * Retorna metadados de paginação (totalPaginas, totalElementos, paginaAtual).
         */
        @GetMapping("/topicos")
        public ResponseEntity<Map<String, Object>> listarTopicos(
                        @RequestParam(required = false) String busca,
                        @RequestParam(required = false) CategoriaForum categoria,
                        @RequestParam(defaultValue = "0") int pagina,
                        @RequestParam(defaultValue = "10") int tamanho) {

                PageRequest pageable = PageRequest.of(pagina, tamanho,
                                Sort.by(Sort.Direction.DESC, "dataCriacao"));
                Page<TopicoForum> page = forumService.listarTopicos(busca, categoria, pageable);

                List<Map<String, Object>> topicos = page.getContent().stream()
                                .map(t -> Map.<String, Object>of(
                                                "id", t.getId(),
                                                "titulo", t.getTitulo(),
                                                "categoria", t.getCategoria().getDescricao(),
                                                "autorNome", t.getAutor().getNome(),
                                                "dataCriacao", t.getDataCriacao().format(FMT),
                                                "qtdRespostas", t.getQtdRespostas(),
                                                "visualizacoes", t.getVisualizacoes(),
                                                "resolvido", t.isResolvido()))
                                .collect(Collectors.toList());

                return ResponseEntity.ok(Map.of(
                                "topicos", topicos,
                                "totalPaginas", page.getTotalPages(),
                                "totalElementos", page.getTotalElements(),
                                "paginaAtual", page.getNumber()));
        }

        // ── POST /api/forum/respostas/{id}/curtir ─────────────────────────────────

        /** Registra ou remove a curtida do cliente autenticado em uma resposta do fórum. */
        @PostMapping("/respostas/{id}/curtir")
        public ResponseEntity<?> curtirResposta(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails user) {

                Long clienteId = clienteRepo.findByEmail(user.getUsername())
                                .map(c -> c.getId())
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

                Map<String, Object> resultado = forumService.curtirResposta(id, clienteId);
                return ResponseEntity.ok(resultado);
        }

        // ── POST /api/forum/respostas/{id}/melhor ─────────────────────────────────

        /**
         * Marca uma resposta como a melhor do tópico; somente o autor do tópico ou um admin pode fazer isso.
         * Retorna 403 se o cliente não tiver permissão.
         */
        @PostMapping("/respostas/{id}/melhor")
        public ResponseEntity<?> marcarMelhorResposta(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails user) {

                Long clienteId = clienteRepo.findByEmail(user.getUsername())
                                .map(c -> c.getId())
                                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

                boolean isAdmin = user.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

                try {
                        forumService.marcarMelhorResposta(id, clienteId, isAdmin);
                        return ResponseEntity.ok(Map.of("mensagem", "Resposta atualizada."));
                } catch (RuntimeException e) {
                        return ResponseEntity.status(403).body(Map.of("erro", e.getMessage()));
                }
        }

        // ── DELETE /api/forum/topicos/{id} — Admin ────────────────────────────────

        /** Remove um tópico do fórum; operação exclusiva para administradores. */
        @DeleteMapping("/topicos/{id}")
        public ResponseEntity<?> deletarTopico(@PathVariable Long id) {
                forumService.deletarTopico(id);
                return ResponseEntity.ok(Map.of("mensagem", "Tópico removido."));
        }

        // ── DELETE /api/forum/respostas/{id} — Admin ou próprio autor ─────────────
        /**
         * Remove uma resposta do fórum; admins podem remover qualquer resposta, clientes apenas as próprias.
         * Retorna 403 se o cliente tentar remover resposta de outro usuário.
         */
        @DeleteMapping("/respostas/{id}")
        public ResponseEntity<?> deletarResposta(
                        @PathVariable Long id,
                        @AuthenticationPrincipal UserDetails user) {

                boolean isAdmin = user.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

                if (!isAdmin) {
                        // Verifica se é o próprio autor
                        boolean ehAutor = forumService.isAutorResposta(id, user.getUsername());
                        if (!ehAutor) {
                                return ResponseEntity.status(403)
                                                .body(Map.of("erro", "Sem permissão para excluir esta resposta."));
                        }
                }

                forumService.deletarResposta(id);
                return ResponseEntity.ok(Map.of("mensagem", "Resposta removida."));
        }
}
