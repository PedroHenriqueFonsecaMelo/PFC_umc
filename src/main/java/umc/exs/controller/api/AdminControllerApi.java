package umc.exs.controller.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.DTOs.compra.LoteExibicaoDTO;
import umc.exs.model.entidades.foundation.Administrador;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.AdminRepository;
import umc.exs.service.core.LivroService;
import umc.exs.service.core.LoteService;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminControllerApi {

    private final LivroService livroService;
    private final AdminRepository adminRepository;
    private final LoteService loteService;

    /**
     * Lista lotes de livros pendentes de aprovação.
     * Retorna DTOs com ID, protocolo, status, data.
     * Acesso admin.
     * 
     * @return List<LoteExibicaoDTO> lotes pendentes
     */
    @GetMapping("/lotes/pendentes")
    public ResponseEntity<List<LoteExibicaoDTO>> listarLotesPendentes() {

        List<LoteExibicaoDTO> lotes = loteService.listarPendentes().stream()
                .map(lote -> new LoteExibicaoDTO(lote.getId(), lote.getCodigoProtocolo(), lote.getStatus().toString(),
                        lote.getDataCriacao()))
                .toList();
        return ResponseEntity.ok(lotes);
    }

    /**
     * Lista livros de um lote pendente.
     * Retorna mapas com id, título, autor, isbn, fotosUrls (lista completa).
     * Para visualização admin.
     * 
     * @param id lote ID
     * @return List<Map> livros lote
     */
    @GetMapping("/lotes/{id}")
    public ResponseEntity<List<Map<String, Object>>> listarLivrosLote(@PathVariable Long id) {

        List<LivroAnuncio> livros = livroService.listarLivrosPorLote(id);

        List<Map<String, Object>> resposta = livros.stream()
                .map(b -> {
                    java.util.HashMap<String, Object> map = new java.util.HashMap<>();
                    map.put("id", b.getId());
                    map.put("titulo", b.getTitulo());
                    map.put("autor", b.getAutor());
                    map.put("isbn", b.getIsbn());

                    map.put("fotosUrls", b.getFotosUrls() != null ? b.getFotosUrls() : "[]");

                    String primeiraFoto = "";
                    if (b.getFotosUrls() != null && b.getFotosUrls().contains("\"")) {
                        primeiraFoto = b.getFotosUrls().split("\"")[1];
                    }
                    map.put("fotoUrl", primeiraFoto);

                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resposta);
    }

    /**
     * Lista livros pendentes de aprovação.
     * Filtrados por aprovado = false.
     * Para painel admin.
     * 
     * @return List<LivroAnuncio> pendentes
     */
    @GetMapping("/livros/pendentes")
    public ResponseEntity<List<LivroAnuncio>> listarLivrosPendentes() {

        // O Service já retorna a lista filtrada usando o loop tradicional
        List<LivroAnuncio> livros = livroService.listarLivrosPendentes();
        return ResponseEntity.ok(livros);
    }

    /**
     * Aprova livro pendente definindo preço/estado.
     * Valida admin autenticado, salva via LivroService.
     * Retorna sucesso ou erro.
     * 
     * @param id        livro ID
     * @param aprovacao DTO estado/preço
     * @param user      admin logado
     */
    @PostMapping("/livros/{id}/aprovar")
    public ResponseEntity<?> aprobarLivro(

            @PathVariable Long id,
            @RequestBody AdminAprovacaoDTO aprovacao,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).body("Acesso negado: Admin não autenticado.");
        }
        if (aprovacao.getEstadoAprovado() == EstadoLivro.RUIM){
            return ResponseEntity.status(401).body("Livro com estado RUIM");
        }

        try {
            // Busca tradicional do Admin
            Optional<Administrador> adminOpt = adminRepository.findByEmail(user.getUsername());

            if (adminOpt.isEmpty()) {
                throw new RuntimeException("A conta de administrador não foi localizada no banco de dados.");
            }

            Long adminId = adminOpt.get().getId();

            // Aqui o service executa a lógica onde o Admin define o preço e estado
            livroService.aprovarLivro(id, adminId, aprovacao);
            return ResponseEntity.ok(Map.of("success", true, "message", "Livro aprovado com sucesso!"));

        } catch (RuntimeException e) {
            // Captura qualquer erro de validação (como preço abusivo) e retorna para o
            // front
            return ResponseEntity.badRequest().body("Erro na aprovação: " + e.getMessage());
        }
    }

    /**
     * Rejeita livro pendente com comentário.
     * Valida admin, chama LivroService rejeitarLivro.
     * Retorna sucesso ou erro.
     * 
     * @param id       livro ID
     * @param rejeicao DTO comentário
     * @param user     admin
     */
    @PostMapping("/livros/{id}/rejeitar")
    public ResponseEntity<?> rejeitarLivro(
            @PathVariable Long id,
            @RequestBody Map<String, String> rejeicao,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).body("Não autenticado");

        try {
            Optional<Administrador> adminOpt = adminRepository.findByEmail(user.getUsername());
            Long adminId = adminOpt.get().getId();
            String comentario = rejeicao.getOrDefault("comentario", "Sem comentário");
            String estado = rejeicao.get("estado");

            // Chame o service
            livroService.rejeitarLivro(id, adminId, estado, comentario);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            // Logue o erro no console para você ver o que é de verdade!
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Erro real: " + e.getMessage());
        }
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller REST API para painel admin aprovação livros.
 * Listagens lotes/livros pendentes, aprovar/rejeitar com validação admin.
 * Integra LivroService, LoteService, AdminRepository.
 * Retorna JSON listas ou mensagens success/error.
 */
