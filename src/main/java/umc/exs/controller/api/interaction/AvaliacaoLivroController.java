package umc.exs.controller.api.interaction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.dtos.livro.AvaliacaoLivroDTO;
import umc.exs.dtos.shared.ComentarioRequestDTO;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.AvaliacaoLivroService;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoLivroController {

    private static final String TITULO = "titulo";
    private static final String AUTOR = "autor";
    private static final String RESUMO_OFICIAL = "resumoOficial";
    private static final String AVALIACOES = "avaliacoes";
    private static final String STATUS = "status";
    private static final String SEM_REFERENCIA_NO_BANCO = "sem_referencia_no_banco";
    private static final String MESSAGE = "message";
    private static final String ERRO = "erro";

    private final AvaliacaoLivroService avaliacaoService;
    private final AvaliacaoLivroRepository avaliacaoRepo;
    private final LivroRepository livroRepo;
    private final ClienteRepository clienteRepo;

    @GetMapping("/livro/{isbn}")
    public ResponseEntity<Map<String, Object>> buscarDadosCentralOpiniao(@PathVariable("isbn") String isbn) {
        try {
            List<AvaliacaoLivro> comentarios = avaliacaoRepo.findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(isbn);
            Map<String, Object> response = new HashMap<>();

            Optional<Livro> livroOficial = livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn);

            if (livroOficial.isPresent()) {
                response.put(TITULO, livroOficial.get().getTitulo());
                response.put(AUTOR, livroOficial.get().getAutor());
                response.put(RESUMO_OFICIAL, livroOficial.get().getResumoOficial());
            } else if (!comentarios.isEmpty()) {
                response.put(TITULO, comentarios.get(0).getTituloLivro());
                response.put(AUTOR, comentarios.get(0).getAutorLivro());
                response.put(RESUMO_OFICIAL, "Dados carregados via histórico da comunidade.");
            } else {
                response.put(TITULO, "");
                response.put(AUTOR, "");
                response.put(RESUMO_OFICIAL, "");
            }

            response.put(AVALIACOES, comentarios);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERRO, "Erro: " + e.getMessage()));
        }
    }

    @GetMapping("/livro/unificado/{isbn}")
    public ResponseEntity<Map<String, Object>> buscarDadosCentralOpiniaoUnificado(@PathVariable("isbn") String isbn) {
        try {
            Optional<Livro> livroOficial = livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn);

            String tituloParaBusca = null;
            String autorParaBusca = null;

            if (livroOficial.isPresent()) {
                tituloParaBusca = livroOficial.get().getTitulo();
                autorParaBusca = livroOficial.get().getAutor();
            } else {
                List<AvaliacaoLivro> comentariosDesteIsbn = avaliacaoRepo
                        .findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(isbn);
                if (!comentariosDesteIsbn.isEmpty()) {
                    tituloParaBusca = comentariosDesteIsbn.get(0).getTituloLivro();
                    autorParaBusca = comentariosDesteIsbn.get(0).getAutorLivro();
                }
            }

            if (tituloParaBusca == null || tituloParaBusca.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        AVALIACOES, List.of(),
                        STATUS, SEM_REFERENCIA_NO_BANCO));
            }

            List<AvaliacaoLivro> todasOsComentariosDoLivro;

            if (autorParaBusca == null || autorParaBusca.isBlank()) {
                todasOsComentariosDoLivro = avaliacaoRepo
                        .findAllByTituloLivroIgnoreCaseOrderByDataAvaliacaoDesc(tituloParaBusca);
            } else {
                todasOsComentariosDoLivro = avaliacaoRepo
                        .findAllByTituloLivroIgnoreCaseAndAutorLivroIgnoreCaseOrderByDataAvaliacaoDesc(
                                tituloParaBusca, autorParaBusca);
            }

            Map<String, Object> response = new HashMap<>();
            response.put(TITULO, tituloParaBusca);
            response.put(AUTOR, autorParaBusca);
            response.put(RESUMO_OFICIAL,
                    livroOficial.map(Livro::getResumoOficial).orElse("Conteúdo compartilhado pela comunidade."));
            response.put(AVALIACOES, todasOsComentariosDoLivro);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERRO, "Erro ao agrupar: " + e.getMessage()));
        }
    }

    @SuppressWarnings("null")
    @PostMapping("/salvar")
    public ResponseEntity<Map<String, Object>> salvarComentario(
            @RequestBody ComentarioRequestDTO payload,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of(ERRO, "Você precisa estar logado."));
        }

        Cliente leitor = clienteRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String isbn = payload.getIsbn();

        Livro livroReferencia = livroRepo.findByIsbn(isbn)
                .orElseGet(() -> avaliacaoService.criarLivroReferencia(payload));

        AvaliacaoLivro nova = AvaliacaoLivro.builder()
                .obra(livroReferencia.getObra())
                .isbnOriginalNoAto(isbn)
                .tituloLivro(payload.getTitulo())
                .autorLivro(payload.getAutor())
                .comentario(payload.getComentario())
                .nota(payload.getNota() != null ? payload.getNota() : 5)
                .dataAvaliacao(LocalDateTime.now())
                .avaliador(leitor)
                .build();

        avaliacaoRepo.save(nova);

        return ResponseEntity.ok(Map.of(
                MESSAGE, "Salvo com sucesso!",
                "id", nova.getId()));
    }

    @GetMapping("/livro/{isbn}/media")
    public ResponseEntity<Map<String, Object>> buscarMedia(@PathVariable("isbn") String isbn) {
        try {
            Double media = avaliacaoService.calcularMediaPorIsbn(isbn);
            if (media == null) {
                return ResponseEntity.ok(Map.of(
                        "media", 0,
                        MESSAGE, "Sem avaliações."));
            }
            return ResponseEntity.ok(Map.of("media", media));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, e.getMessage()));
        }
    }

    @PutMapping("/admin/resumo/{isbn}")
    public ResponseEntity<Map<String, Object>> atualizarResumoAdmin(
            @PathVariable("isbn") String isbn,
            @RequestBody Map<String, String> payload) {

        Optional<Livro> livroOpt = livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn);

        if (livroOpt.isPresent()) {
            Livro livro = livroOpt.get();
            livro.setResumoOficial(payload.get("resumo"));
            livroRepo.save(livro);

            // Correção: Map.of retorna Map<String, String>, aqui forçamos Object para casar
            // com a assinatura
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE, "Resumo oficial atualizado com sucesso.");
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(404).body(Map.of(ERRO, "Livro não encontrado."));
    }

    @PostMapping("/legado")
    public ResponseEntity<Map<String, Object>> criarAvaliacao(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody AvaliacaoLivroDTO dto) {

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of(ERRO, "Não autorizado."));
        }

        try {
            AvaliacaoLivro avaliacao = Objects.requireNonNull(avaliacaoService.criarAvaliacao(user.getUsername(), dto));
            return ResponseEntity.ok(Map.of(
                    MESSAGE, "Avaliação criada com sucesso.",
                    "avaliacao", avaliacao));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERRO, e.getMessage()));
        }
    }
}