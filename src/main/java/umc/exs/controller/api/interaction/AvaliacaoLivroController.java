package umc.exs.controller.api.interaction;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import umc.exs.DTOs.livro.AvaliacaoLivroDTO;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.livro.ObraRpository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.AvaliacaoLivroService;

@RestController
@RequestMapping("/api/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoLivroController {

    private final AvaliacaoLivroService avaliacaoService;
    private final AvaliacaoLivroRepository avaliacaoRepo;
    private final LivroRepository livroRepo;
    private final ClienteRepository clienteRepo;
    private final ObraRpository obraRepo;

    @GetMapping("/livro/{isbn}")
    public ResponseEntity<?> buscarDadosCentralOpiniao(@PathVariable("isbn") String isbn) {
        try {
            // Ajustado para o novo nome do método no Repository
            List<AvaliacaoLivro> comentarios = avaliacaoRepo.findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(isbn);
            Map<String, Object> response = new HashMap<>();

            Optional<Livro> livroOficial = livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn);

            if (livroOficial.isPresent()) {
                response.put("titulo", livroOficial.get().getTitulo());
                response.put("autor", livroOficial.get().getAutor());
                response.put("resumoOficial", livroOficial.get().getResumoOficial());
            } else if (!comentarios.isEmpty()) {
                response.put("titulo", comentarios.get(0).getTituloLivro());
                response.put("autor", comentarios.get(0).getAutorLivro());
                response.put("resumoOficial", "Dados carregados via histórico da comunidade.");
            } else {
                response.put("titulo", "");
                response.put("autor", "");
                response.put("resumoOficial", "");
            }

            response.put("avaliacoes", comentarios);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/livro/unificado/{isbn}")
    public ResponseEntity<?> buscarDadosCentralOpiniaoUnificado(@PathVariable("isbn") String isbn) {
        try {
            // 1. Tenta achar o livro oficial
            Optional<Livro> livroOficial = livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn);

            String tituloParaBusca = null;
            String autorParaBusca = null;

            if (livroOficial.isPresent()) {
                tituloParaBusca = livroOficial.get().getTitulo();
                autorParaBusca = livroOficial.get().getAutor();
            } else {
                // 2. Se não tem livro oficial, busca qualquer avaliação desse ISBN para
                // descobrir o título
                List<AvaliacaoLivro> comentariosDesteIsbn = avaliacaoRepo
                        .findByIsbnOriginalNoAtoOrderByDataAvaliacaoDesc(isbn);
                if (!comentariosDesteIsbn.isEmpty()) {
                    tituloParaBusca = comentariosDesteIsbn.get(0).getTituloLivro();
                    autorParaBusca = comentariosDesteIsbn.get(0).getAutorLivro();
                }
            }

            // 3. Se não achamos título nem por livro oficial nem por avaliação prévia,
            // retornamos vazio
            if (tituloParaBusca == null || tituloParaBusca.isEmpty()) {
                return ResponseEntity.ok(Map.of("avaliacoes", List.of(), "status", "sem_referencia_no_banco"));
            }

            // 4. Busca unificada (O trim ajuda a evitar erros de espaço em branco)
            List<AvaliacaoLivro> todasOsComentariosDoLivro;

            if (autorParaBusca == null || autorParaBusca.isBlank()) {
                // Se não temos autor, buscamos apenas pelo título
                todasOsComentariosDoLivro = avaliacaoRepo
                        .findAllByTituloLivroIgnoreCaseOrderByDataAvaliacaoDesc(tituloParaBusca);
            } else {
                // Se temos autor, fazemos a busca completa
                todasOsComentariosDoLivro = avaliacaoRepo
                        .findAllByTituloLivroIgnoreCaseAndAutorLivroIgnoreCaseOrderByDataAvaliacaoDesc(
                                tituloParaBusca, autorParaBusca);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("titulo", tituloParaBusca);
            response.put("autor", autorParaBusca);
            response.put("resumoOficial",
                    livroOficial.map(Livro::getResumoOficial).orElse("Conteúdo compartilhado pela comunidade."));
            response.put("avaliacoes", todasOsComentariosDoLivro);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace(); // Isso vai mostrar o erro real no console do Java
            return ResponseEntity.status(500).body("Erro ao agrupar: " + e.getMessage());
        }
    }

    @SuppressWarnings("null")
    @PostMapping("/salvar")
    public ResponseEntity<?> salvarComentario(@RequestBody Map<String, Object> payload, Authentication auth) {
        try {
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(401).body("Você precisa estar logado.");
            }

            Cliente leitor = clienteRepo.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + auth.getName()));

            String isbn = (String) payload.get("isbn");

            Livro livroReferencia = livroRepo.findByIsbn(isbn)
                    .orElseGet(() -> {
                        Obra novaObra = obraRepo.save(Obra.builder()
                                .tituloOriginal((String) payload.get("titulo"))
                                .autor((String) payload.get("autor"))
                                .build());

                        return livroRepo.save(Livro.builder()
                                .isbn(isbn)
                                .titulo((String) payload.get("titulo"))
                                .obra(novaObra)
                                .build());
                    });

            AvaliacaoLivro nova = AvaliacaoLivro.builder()
                    .obra(livroReferencia.getObra())
                    .isbnOriginalNoAto(isbn)
                    .tituloLivro((String) payload.get("titulo"))
                    .autorLivro((String) payload.get("autor"))
                    .comentario((String) payload.get("comentario"))
                    .nota(payload.get("nota") != null ? Integer.parseInt(payload.get("nota").toString()) : 5)
                    .dataAvaliacao(LocalDateTime.now())
                    .avaliador(leitor)
                    .build();

            avaliacaoRepo.save(nova);
            return ResponseEntity.ok(Map.of("message", "Salvo com sucesso!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @GetMapping("/livro/{isbn}/media")
    public ResponseEntity<?> buscarMedia(@PathVariable("isbn") String isbn) {
        try {
            Double media = avaliacaoService.calcularMediaPorIsbn(isbn);
            if (media == null) {
                return ResponseEntity.ok(Map.of("media", 0, "mensagem", "Sem avaliações."));
            }
            return ResponseEntity.ok(Map.of("media", media));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/admin/resumo/{isbn}")
    public ResponseEntity<?> atualizarResumoAdmin(@PathVariable("isbn") String isbn,
            @RequestBody Map<String, String> payload) {
        return livroRepo.findFirstByIsbnOrderByDataAprovacaoDesc(isbn)
                .map(livro -> {
                    livro.setResumoOficial(payload.get("resumo"));
                    livroRepo.save(livro);
                    return ResponseEntity.ok("Resumo oficial atualizado com sucesso.");
                })
                .orElse(ResponseEntity.status(404).body("Livro não encontrado."));
    }

    @PostMapping("/legado")
    public ResponseEntity<?> criarAvaliacao(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody AvaliacaoLivroDTO dto) {

        if (user == null)
            return ResponseEntity.status(401).body("Não autorizado.");

        try {
            AvaliacaoLivro avaliacao = avaliacaoService.criarAvaliacao(user.getUsername(), dto);
            return ResponseEntity.ok(avaliacao);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}