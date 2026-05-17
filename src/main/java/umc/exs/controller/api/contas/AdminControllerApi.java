package umc.exs.controller.api.contas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

import umc.exs.service.core.control.ClienteAdminService;
import umc.exs.dto.admin.AdminAprovacaoDTO;
import umc.exs.dto.admin.ClienteListaDTO;
import umc.exs.dto.admin.ClientePerfilDTO;
import umc.exs.dto.admin.DashboardMetricasDTO;
import umc.exs.dto.admin.LivroAdminRequest;
import umc.exs.dto.admin.RejeicaoLivroDTO;
import umc.exs.dto.compra.AtualizarEnvioDTO;
import umc.exs.dto.compra.CriarCupomDTO;
import umc.exs.dto.compra.PedidoDTO;
import umc.exs.dto.compra.cupom.CupomDTO;
import umc.exs.dto.compra.lote.LoteExibicaoDTO;
import umc.exs.dto.livro.LivroDTO;
import umc.exs.dto.shared.ApiResponseDTO;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.service.core.bussiness.LivroService;
import umc.exs.service.core.control.DashboardService;
import umc.exs.service.core.control.LoteService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.core.interactions.PostBlogService;
import umc.exs.service.cupom.CupomService;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminControllerApi {

    private final LivroService livroService;
    private final LivroRepository livroRepository;
    private final AdminRepository adminRepository;
    private final LoteService loteService;
    private final PedidoService pedidoService;
    private final DashboardService dashboardService;
    private final CupomService cupomService;
    private final PostBlogService postBlogService;
    private final ClienteAdminService clienteAdminService;

    private static final String NAO_AUTENTICADO = "Acesso negado: Admin não autenticado.";
    private static final String ADMIN_NAO_ENCONTRADO = "Conta de administrador não encontrada.";

    // ==========================================================
    // LOTES
    // ==========================================================

    @GetMapping("/lotes/pendentes")
    public ResponseEntity<List<LoteExibicaoDTO>> listarLotesPendentes() {
        List<LoteExibicaoDTO> dtos = loteService.listarPendentesComCliente().stream()
                .map(lote -> {
                    long qtd = livroRepository.countByLoteId(lote.getId());
                    String nome  = lote.getCliente() != null ? lote.getCliente().getNome()  : "—";
                    String email = lote.getCliente() != null ? lote.getCliente().getEmail() : "—";
                    return new LoteExibicaoDTO(lote.getId(), lote.getCodigoProtocolo(),
                            lote.getStatus().toString(), lote.getDataCriacao(), nome, email, qtd);
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/lotes/{id}/detalhes")
    public ResponseEntity<Map<String, Object>> detalharLote(@PathVariable Long id) {
        Lote lote = loteService.findByIdComCliente(id);
        List<LivroDTO> livros = livroService.listarLivrosPorLote(id);

        java.util.LinkedHashMap<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("id", lote.getId());
        resp.put("codigoProtocolo", lote.getCodigoProtocolo());
        resp.put("status", lote.getStatus().toString());
        resp.put("dataCriacao", lote.getDataCriacao());
        resp.put("nomeVendedor",  lote.getCliente() != null ? lote.getCliente().getNome()  : "—");
        resp.put("emailVendedor", lote.getCliente() != null ? lote.getCliente().getEmail() : "—");
        resp.put("vendedorId",    lote.getCliente() != null ? lote.getCliente().getId()     : null);

        List<Map<String, Object>> livrosMaps = livros.stream().map(b -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id",        b.getId());
            map.put("titulo",    b.getTitulo());
            map.put("autor",     b.getAutor());
            map.put("isbn",      b.getIsbn());
            map.put("fotosUrls", b.getFotosUrls() != null ? b.getFotosUrls() : "[]");
            return map;
        }).toList();
        resp.put("livros", livrosMaps);
        resp.put("quantidadeLivros", livrosMaps.size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/lotes/{id}")
    public ResponseEntity<List<Map<String, Object>>> listarLivrosLote(@PathVariable Long id) {
        List<LivroDTO> livros = livroService.listarLivrosPorLote(id);
        List<Map<String, Object>> resposta = livros.stream().map(b -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("titulo", b.getTitulo());
            map.put("autor", b.getAutor());
            map.put("isbn", b.getIsbn());
            map.put("fotosUrls", b.getFotosUrls() != null ? b.getFotosUrls() : "[]");
            return map;
        }).toList();
        return ResponseEntity.ok(resposta);
    }

    // ==========================================================
    // LIVROS (Aprovação e Gestão)
    // ==========================================================

    @GetMapping("/livros/pendentes")
    public ResponseEntity<List<LivroDTO>> listarLivrosPendentes() {
        return ResponseEntity.ok(livroService.listarLivrosPendentes());
    }

    @PostMapping("/livros/{id}/aprovar")
    public ResponseEntity<ApiResponseDTO> aprovarLivro(
            @PathVariable Long id,
            @RequestBody AdminAprovacaoDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        livroService.aprovarLivro(id, admin.getId(), dto);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro aprovado com sucesso"));
    }

    @PostMapping("/livros/{id}/rejeitar")
    public ResponseEntity<ApiResponseDTO> rejeitarLivro(
            @PathVariable Long id,
            @RequestBody RejeicaoLivroDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        livroService.rejeitarLivro(id, admin.getId(), dto.getEstado(), dto.getComentario());
        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro rejeitado com sucesso"));
    }

    @GetMapping("/livros/aprovados")
    public ResponseEntity<List<LivroDTO>> listarLivrosAprovados() {
        return ResponseEntity.ok(livroService.listarLivrosAprovados());
    }

    @PostMapping("/livros/novo")
    public ResponseEntity<ApiResponseDTO> adicionarLivro(
            @RequestBody LivroAdminRequest request, // Use o Request aqui
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        request.setAdminId(admin.getId());

        livroService.adicionarLivroAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO(true, "Livro adicionado ao estoque"));
    }

    @PutMapping("/livros/{id}")
    public ResponseEntity<ApiResponseDTO> editarLivro(
            @PathVariable @NonNull Long id,
            @RequestBody LivroAdminRequest request,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));

        livroService.editarLivroAdmin(id, request);

        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro atualizado"));
    }

    @DeleteMapping("/livros/{id}")
    public ResponseEntity<ApiResponseDTO> deletarLivro(
            @PathVariable @NonNull Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));

        livroService.deletarLivroAdmin(id);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro removido"));
    }

    // ==========================================================
    // PEDIDOS
    // ==========================================================

    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoDTO>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @PostMapping("/pedidos/{id}/envio")
    public ResponseEntity<PedidoDTO> atualizarEnvio(
            @PathVariable Long id,
            @RequestBody AtualizarEnvioDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_AUTENTICADO);

        return ResponseEntity.ok(pedidoService.atualizarStatus(id, dto.getStatusEnvio(), dto.getCodigoRastreio()));
    }

    // ==========================================================
    // CUPONS
    // ==========================================================

    @GetMapping("/cupons")
    public ResponseEntity<List<CupomDTO>> listarCupons() {
        return ResponseEntity.ok(cupomService.listarTodosCupons());
    }

    @DeleteMapping("/cupons/{id}")
    public ResponseEntity<ApiResponseDTO> invalidarCupom(@PathVariable @NonNull Long id) {
        cupomService.invalidarCupom(Objects.requireNonNull(id, "ID não pode ser nulo"));
        return ResponseEntity.ok(new ApiResponseDTO(true, "Cupom invalidado"));
    }

    @PostMapping("/cupons")
    public ResponseEntity<?> criarCupom(@RequestBody CriarCupomDTO dto) {
        try {
            if (dto.getDataValidade() == null) {
                return ResponseEntity.badRequest().body(new ApiResponseDTO(false, "Data de validade obrigatória"));
            }
            LocalDateTime data = LocalDateTime.parse(dto.getDataValidade());
            Cupom cupom = cupomService.criarCupom(dto, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(cupom);

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO(false, "Formato de data inválido. Use ISO-8601."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponseDTO(false, e.getMessage()));
        }
    }

    // ==========================================================
    // DASHBOARD
    // ==========================================================

    @GetMapping("/dashboard/metricas")
    public ResponseEntity<DashboardMetricasDTO> getMetricas() {
        return ResponseEntity.ok(dashboardService.getMetricas());
    }

    // ==========================================================
    // SESSÃO
    // ==========================================================

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getMe(@AuthenticationPrincipal UserDetails user) {
        String nome = adminRepository.findByEmail(user.getUsername())
                .map(a -> a.getNome())
                .orElse("Administrador");
        return ResponseEntity.ok(Map.of("nome", nome, "role", "ADMIN"));
    }

    // ==========================================================
    // CLIENTES
    // ==========================================================

    @GetMapping("/clientes")
    public ResponseEntity<List<ClienteListaDTO>> listarClientes() {
        return ResponseEntity.ok(clienteAdminService.listarClientes());
    }

    @GetMapping("/clientes/{id}")
    public ResponseEntity<?> getPerfilCliente(@PathVariable Long id) {
        try {
            ClientePerfilDTO perfil = clienteAdminService.getPerfilCliente(id);
            return ResponseEntity.ok(perfil);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO(false, e.getMessage()));
        }
    }

    // ==========================================================
    // BLOG
    // ==========================================================

    @GetMapping("/blog")
    public ResponseEntity<List<Map<String, Object>>> listarPostsBlog() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> posts = postBlogService.listarTodos().stream()
                .map(p -> {
                    java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", p.getId());
                    m.put("titulo", p.getTitulo() != null ? p.getTitulo() : "");
                    m.put("conteudo", p.getConteudo() != null ? p.getConteudo() : "");
                    m.put("imagemUrl", p.getImagemUrl() != null ? p.getImagemUrl() : "");
                    m.put("autorNome", p.getAutorNome() != null ? p.getAutorNome() : "Administrador");
                    m.put("dataPublicacao", p.getDataPublicacao() != null ? p.getDataPublicacao().format(fmt) : "");
                    m.put("curtidas", p.getCurtidas());
                    m.put("status", p.getStatus() != null ? p.getStatus().name() : "PUBLICADO");
                    return (Map<String, Object>) m;
                }).toList();
        return ResponseEntity.ok(posts);
    }
}