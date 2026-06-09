package umc.exs.controller.api.contas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Objects;

import umc.exs.dto.mapper.CupomMapper;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.model.entidades.logic.ReporteResposta;
import umc.exs.repository.logic.ReporteRepository;
import umc.exs.repository.logic.ReporteRespostaRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.dto.mapper.PedidoMapper;
import umc.exs.dto.request.admin.AdminAprovacaoRequest;
import umc.exs.dto.request.admin.AtualizarEnvioRequest;
import umc.exs.dto.request.admin.CriarCupomRequest;
import umc.exs.dto.request.admin.LivroAdminRequest;
import umc.exs.dto.request.admin.RemoverClienteRequest;
import umc.exs.dto.request.admin.SuspenderClienteRequest;
import umc.exs.dto.request.livro.RejeicaoLivroRequest;
import umc.exs.dto.response.admin.ExternApiResponse;
import umc.exs.dto.response.admin.DashboardResponse;
import umc.exs.dto.response.cliente.ClienteListaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.dto.response.compras.CupomResponse;
import umc.exs.dto.response.compras.LivroExibicaoResponse;
import umc.exs.dto.response.compras.LoteResponse;
import umc.exs.dto.response.compras.PedidoResponse;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.service.cliente.admin.ClienteAdminService;
import umc.exs.service.core.dashboard.DashboardService;
import umc.exs.service.core.dashboard.LoteService;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.core.interactions.PostBlogService;
import umc.exs.service.core.livros.LivroService;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.storage.ArquivosService;

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

    private final LivroMapper livroMapper;
    private final PedidoMapper pedidoMapper;
    private final CupomMapper cupomMapper;
    private final ReporteRepository reporteRepository;
    private final ReporteRespostaRepository reporteRespostaRepository;
    private final EmailFacade emailFacade;

    // ==========================================================
    // LOTES
    // ==========================================================

    @GetMapping("/lotes/pendentes")
    public ResponseEntity<List<LoteResponse>> listarLotesPendentes() {
        List<LoteResponse> dtos = loteService.listarPendentesComCliente().stream()
                .map(lote -> {
                    long qtd = livroRepository.countByLoteId(lote.getId());
                    String nome = lote.getCliente() != null ? lote.getCliente().getNome() : "—";
                    String email = lote.getCliente() != null ? lote.getCliente().getEmail() : "—";
                    return new LoteResponse(lote.getId(), lote.getCodigoProtocolo(),
                            lote.getStatus().toString(), lote.getDataCriacao(), nome, email, qtd);
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/lotes/{id}/detalhes")
    public ResponseEntity<Map<String, Object>> detalharLote(@PathVariable Long id) {
        Lote lote = loteService.findByIdComCliente(id);

        LinkedHashMap<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", lote.getId());
        resp.put("codigoProtocolo", lote.getCodigoProtocolo());
        resp.put("status", lote.getStatus().toString());
        resp.put("dataCriacao", lote.getDataCriacao());
        resp.put("nomeVendedor", lote.getCliente() != null ? lote.getCliente().getNome() : "—");
        resp.put("emailVendedor", lote.getCliente() != null ? lote.getCliente().getEmail() : "—");
        resp.put("vendedorId", lote.getCliente() != null ? lote.getCliente().getId() : null);

        List<Map<String, Object>> livrosMaps = livroService.listarLivrosPorLote(id).stream().map(b -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", b.getId());
            map.put("titulo", b.getTitulo());
            map.put("autor", b.getAutor());
            map.put("isbn", b.getIsbn());
            map.put("fotosUrls", b.getFotosUrls() != null ? b.getFotosUrls() : "[]");
            return map;
        }).toList();
        resp.put("livros", livrosMaps);
        resp.put("quantidadeLivros", livrosMaps.size());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/lotes/{id}")
    public ResponseEntity<List<Map<String, Object>>> listarLivrosLote(@PathVariable Long id) {
        List<Map<String, Object>> resposta = livroService.listarLivrosPorLote(id).stream().map(b -> {
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
    public ResponseEntity<List<LivroExibicaoResponse>> listarLivrosPendentes() {
        return ResponseEntity.ok(livroMapper.toResponseList(livroService.listarLivrosPendentes()));
    }

    @PostMapping("/livros/{id}/aprovar")
    public ResponseEntity<ExternApiResponse<Void>> aprovarLivro(
            @PathVariable Long id,
            @RequestBody @Valid AdminAprovacaoRequest dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ExternApiResponse.fail(NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        livroService.aprovarLivro(id, admin.getId(), dto);
        return ResponseEntity.ok(ExternApiResponse.ok("Livro aprovado com sucesso"));
    }

    @PostMapping("/livros/{id}/rejeitar")
    public ResponseEntity<ExternApiResponse<Void>> rejeitarLivro(
            @PathVariable Long id,
            @RequestBody @Valid RejeicaoLivroRequest dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ExternApiResponse.fail(NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        livroService.rejeitarLivro(id, admin.getId(), dto.getEstado(), dto.getComentario());
        return ResponseEntity.ok(ExternApiResponse.ok("Livro rejeitado com sucesso"));
    }

    @GetMapping("/livros/aprovados")
    public ResponseEntity<List<LivroExibicaoResponse>> listarLivrosAprovados() {
        return ResponseEntity.ok(livroMapper.toResponseList(livroService.listarLivrosAprovados()));
    }

    @PostMapping("/livros/novo")
    public ResponseEntity<ExternApiResponse<Void>> adicionarLivro(
            @RequestBody LivroAdminRequest request, // Use o Request aqui
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ExternApiResponse.fail(NAO_AUTENTICADO));

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException(ADMIN_NAO_ENCONTRADO));

        request.setAdminId(admin.getId());

        livroService.adicionarLivroAdmin(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ExternApiResponse.ok("Livro adicionado ao estoque"));
    }

    @PutMapping("/livros/{id}")
    public ResponseEntity<ExternApiResponse<Void>> editarLivro(
            @PathVariable @NonNull Long id,
            @RequestBody @Valid LivroAdminRequest request,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ExternApiResponse.fail(NAO_AUTENTICADO));

        livroService.editarLivroAdmin(id, request);

        return ResponseEntity.ok(ExternApiResponse.ok("Livro atualizado"));
    }

    @DeleteMapping("/livros/{id}")
    public ResponseEntity<ExternApiResponse<Void>> deletarLivro(
            @PathVariable @NonNull Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ExternApiResponse.fail(NAO_AUTENTICADO));

        livroService.deletarLivroAdmin(id);
        return ResponseEntity.ok(ExternApiResponse.ok("Livro removido"));
    }

    // ==========================================================
    // PEDIDOS
    // ==========================================================

    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoResponse>> listarPedidos() {
        return ResponseEntity.ok(pedidoMapper.toResponseList(pedidoService.listarTodos()));
    }

    @PostMapping("/pedidos/{id}/envio")
    public ResponseEntity<PedidoResponse> atualizarEnvio(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarEnvioRequest dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, NAO_AUTENTICADO);

        return ResponseEntity.ok(pedidoMapper
                .toResponse(pedidoService.atualizarStatus(id, dto.getStatusEnvio(), dto.getCodigoRastreio())));
    }

    // ==========================================================
    // CUPONS
    // ==========================================================

    @GetMapping("/cupons")
    public ResponseEntity<List<CupomResponse>> listarCupons() {
        return ResponseEntity.ok(cupomMapper.toResponseList(cupomService.listarTodosCupons()));
    }

    @DeleteMapping("/cupons/{id}")
    public ResponseEntity<ExternApiResponse<Void>> invalidarCupom(@PathVariable @NonNull Long id) {
        cupomService.invalidarCupom(Objects.requireNonNull(id, "ID não pode ser nulo"));
        return ResponseEntity.ok(ExternApiResponse.ok("Cupom invalidado"));
    }

    @PostMapping("/cupons")
    public ResponseEntity<?> criarCupom(@RequestBody @Valid CriarCupomRequest dto) {
        try {
            if (dto.getDataValidade() == null) {
                return ResponseEntity.badRequest().body(ExternApiResponse.fail("Data de validade obrigatória"));
            }
            LocalDateTime data = LocalDateTime.parse(dto.getDataValidade());
            Cupom cupom = cupomService.criarCupom(dto, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(cupom);

        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(ExternApiResponse.fail("Formato de data inválido. Use ISO-8601."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ExternApiResponse.fail(e.getMessage()));
        }
    }

    // ==========================================================
    // DASHBOARD
    // ==========================================================

    @GetMapping("/dashboard/metricas")
    public ResponseEntity<DashboardResponse> getMetricas() {
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
    public ResponseEntity<List<ClienteListaResponse>> listarClientes() {
        return ResponseEntity.ok(clienteAdminService.listarClientes());
    }

    @GetMapping("/clientes/{id}")
    public ResponseEntity<?> getPerfilCliente(@PathVariable Long id) {
        try {
            ClientePerfilResponse perfil = clienteAdminService.getPerfilCliente(id);
            return ResponseEntity.ok(perfil);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExternApiResponse.fail(e.getMessage()));
        }
    }

    @PostMapping("/clientes/{id}/suspender")
    public ResponseEntity<?> suspenderCliente(
            @PathVariable Long id,
            @RequestBody SuspenderClienteRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String adminEmail = userDetails != null ? userDetails.getUsername() : "admin";
            clienteAdminService.suspenderCliente(id, req.getMotivo(), req.getDiasSuspensao(), req.isNotificarEmail(), adminEmail);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/clientes/{id}/remover")
    public ResponseEntity<?> removerCliente(
            @PathVariable Long id,
            @RequestBody RemoverClienteRequest req,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String adminEmail = userDetails != null ? userDetails.getUsername() : "admin";
            clienteAdminService.removerCliente(id, req.getMotivo(), req.isNotificarEmail(), adminEmail);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
        }
    }

    @PostMapping("/clientes/{id}/reativar")
    public ResponseEntity<?> reativarCliente(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            String mensagem = body != null ? (String) body.get("mensagem") : null;
            boolean notificar = body != null && Boolean.TRUE.equals(body.get("notificarEmail"));
            clienteAdminService.reativarCliente(id, mensagem, notificar);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
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

    // ==========================================================
    // REPORTES
    // ==========================================================

    @GetMapping("/reportes")
    public ResponseEntity<List<Map<String, Object>>> listarReportes() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        List<Map<String, Object>> lista = reporteRepository.findAllByOrderByDataCriacaoDesc()
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("emailContato", r.getEmailContato());
                    m.put("motivo", r.getMotivo());
                    m.put("detalhes", r.getDetalhes() != null ? r.getDetalhes() : "");
                    m.put("dataCriacao", r.getDataCriacao() != null ? r.getDataCriacao().format(fmt) : "");
                    m.put("lido", r.isLido());
                    m.put("nomeUsuario", r.getNomeUsuario() != null ? r.getNomeUsuario() : "—");
                    m.put("statusConta", r.getStatusConta() != null ? r.getStatusConta() : "—");
                    m.put("dataCadastro", r.getDataCadastro() != null ? r.getDataCadastro().format(fmt) : "—");
                    List<Map<String, Object>> respostas = reporteRespostaRepository
                            .findByReporteIdOrderByDataEnvioAsc(r.getId())
                            .stream().map(resp -> {
                                Map<String, Object> rm = new LinkedHashMap<>();
                                rm.put("mensagem", resp.getMensagem());
                                rm.put("dataEnvio", resp.getDataEnvio() != null
                                        ? resp.getDataEnvio().format(fmt) : "");
                                return rm;
                            }).toList();
                    m.put("respostas", respostas);
                    return m;
                }).toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/reportes/nao-lidos/count")
    public ResponseEntity<Map<String, Object>> contarReportesNaoLidos() {
        return ResponseEntity.ok(Map.of("count", reporteRepository.countByLidoFalse()));
    }

    @PostMapping("/reportes/{id}/marcar-lido")
    public ResponseEntity<?> marcarReporteLido(@PathVariable Long id) {
        return reporteRepository.findById(id).map(r -> {
            r.setLido(true);
            reporteRepository.save(r);
            return ResponseEntity.ok(Map.of("ok", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/reportes/{id}/responder")
    public ResponseEntity<?> responderReporte(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return reporteRepository.findById(id).map(r -> {
            String mensagem = body.getOrDefault("mensagem", "");
            String html = EmailHtmlBuilder.respostaReporte(mensagem);
            emailFacade.sendHtmlSafe(r.getEmailContato(),
                    "Retorno sobre seu reporte — Bibliotroca", html);
            reporteRespostaRepository.save(ReporteResposta.builder()
                    .reporte(r)
                    .mensagem(mensagem)
                    .dataEnvio(LocalDateTime.now())
                    .build());
            r.setLido(true);
            reporteRepository.save(r);
            return ResponseEntity.ok(Map.of("ok", true));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/reportes/{id}")
    public ResponseEntity<?> excluirReporte(@PathVariable Long id) {
        if (!reporteRepository.existsById(id)) return ResponseEntity.notFound().build();
        reporteRespostaRepository.findByReporteIdOrderByDataEnvioAsc(id)
                .forEach(reporteRespostaRepository::delete);
        reporteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping(value = "/livros/upload-foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadFotoLivro(
            @RequestParam("foto") MultipartFile foto,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String url = ArquivosService.salvarFotoLivro(foto, "uploads/livros/");
        return ResponseEntity.ok(Map.of("url", url));
    }

    
}