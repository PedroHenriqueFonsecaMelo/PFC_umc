package umc.exs.controller.api.contas;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.DTOs.admin.DashboardMetricasDTO;
import umc.exs.DTOs.compra.AtualizarEnvioDTO;
import umc.exs.DTOs.compra.LoteExibicaoDTO;
import umc.exs.DTOs.compra.PedidoDTO;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.DTOs.compra.CupomDTO;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.service.core.cliente.LivroService;
import umc.exs.service.core.control.DashboardService;
import umc.exs.service.core.control.LoteService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.cupom.CupomService;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminControllerApi {

    private final LivroService livroService;
    private final AdminRepository adminRepository;
    private final LoteService loteService;
    private final PedidoService pedidoService;
    private final DashboardService dashboardService;
    private final CupomService cupomService;

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

        List<Livro> livros = livroService.listarLivrosPorLote(id);

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
    public ResponseEntity<List<Livro>> listarLivrosPendentes() {

        // O Service já retorna a lista filtrada usando o loop tradicional
        List<Livro> livros = livroService.listarLivrosPendentes();
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
        if (aprovacao.getEstadoAprovado() == EstadoLivro.RUIM) {
            return ResponseEntity.status(400).body("Livros com estado RUIM devem ser rejeitados, não aprovados.");
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
            if (adminOpt.isEmpty()) {
                return ResponseEntity.status(401).body("Conta de administrador não encontrada.");
            }
            Long adminId = adminOpt.get().getId();
            String comentario = rejeicao.getOrDefault("comentario", "Sem comentário");
            String estado = rejeicao.get("estado");

            // Chame o service
            livroService.rejeitarLivro(id, adminId, estado, comentario);

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Erro ao rejeitar livro ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Erro ao rejeitar livro: " + e.getMessage());
        }
    }

    // ==========================================================
    // PEDIDOS — Gestão de Envio
    // ==========================================================

    /**
     * Lista todos os pedidos do sistema para o admin.
     * Retorna id, comprador, livro, status de envio e data.
     */
    @GetMapping("/pedidos")
    public ResponseEntity<List<PedidoDTO>> listarTodosPedidos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    /**
     * Atualiza o status de envio de um pedido.
     * Permite avançar para EM_TRANSITO, ENTREGUE ou CANCELADO.
     * Opcionalmente registra código de rastreio.
     */
    @PostMapping("/pedidos/{id}/envio")
    public ResponseEntity<?> atualizarEnvio(
            @PathVariable Long id,
            @RequestBody AtualizarEnvioDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).body("Não autenticado.");

        try {
            PedidoDTO atualizado = pedidoService.atualizarStatus(id, dto.getStatusEnvio(), dto.getCodigoRastreio());
            return ResponseEntity.ok(atualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================================
    // ESTOQUE — CRUD livros aprovados
    // ==========================================================

    @GetMapping("/livros/aprovados")
    public ResponseEntity<List<Map<String, Object>>> listarLivrosAprovados() {
        List<Map<String, Object>> lista = livroService.listarLivrosAprovados().stream()
                .map(b -> {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("id",             b.getId());
                    m.put("titulo",         b.getTitulo());
                    m.put("autor",          b.getAutor());
                    m.put("isbn",           b.getIsbn());
                    m.put("precoAprovado",  b.getPrecoAprovado());
                    m.put("estadoAprovado", b.getEstadoAprovado() != null ? b.getEstadoAprovado().name() : null);
                    m.put("resumoOficial",  b.getResumoOficial());
                    m.put("fotosUrls",      b.getFotosUrls() != null ? b.getFotosUrls() : "[]");
                    String primeiraFoto = "";
                    if (b.getFotosUrls() != null && b.getFotosUrls().contains("\"")) {
                        primeiraFoto = b.getFotosUrls().split("\"")[1];
                    }
                    m.put("fotoUrl",        primeiraFoto);
                    m.put("emPromocao",     b.getEmPromocao() != null ? b.getEmPromocao() : false);
                    m.put("precoOriginal",  b.getPrecoOriginal());
                    m.put("promocaoExpira", b.getPromocaoExpira() != null
                            ? b.getPromocaoExpira().toString() : null);
                    return m;
                })
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/livros/novo")
    public ResponseEntity<?> adicionarLivro(@RequestBody Map<String, Object> body,
                                             @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("Não autenticado.");
        try {
            Optional<Administrador> adminOpt = adminRepository.findByEmail(user.getUsername());
            if (adminOpt.isEmpty()) return ResponseEntity.status(401).body("Admin não encontrado.");
            Long adminId = adminOpt.get().getId();

            String titulo  = (String) body.get("titulo");
            String autor   = (String) body.get("autor");
            String isbn    = (String) body.getOrDefault("isbn", "");
            Double preco   = body.get("preco") != null ? ((Number) body.get("preco")).doubleValue() : 0.0;
            String estado  = (String) body.getOrDefault("estado", "BOM");
            String resumo  = (String) body.getOrDefault("resumo", "");

            if (titulo == null || titulo.isBlank()) return ResponseEntity.badRequest().body("Título obrigatório.");
            if (autor  == null || autor.isBlank())  return ResponseEntity.badRequest().body("Autor obrigatório.");

            umc.exs.model.enums.EstadoLivro estadoEnum = umc.exs.model.enums.EstadoLivro.valueOf(estado);
            var livro = livroService.adicionarLivroAdmin(titulo, autor, isbn, preco, estadoEnum, resumo, adminId);
            return ResponseEntity.ok(Map.of("success", true, "id", livro.getId(), "message", "Livro adicionado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @PutMapping("/livros/{id}")
    public ResponseEntity<?> editarLivro(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body,
                                          @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("Não autenticado.");
        try {
            String titulo = (String) body.get("titulo");
            String autor  = (String) body.get("autor");
            String isbn   = (String) body.get("isbn");
            Double preco  = body.get("preco") != null ? ((Number) body.get("preco")).doubleValue() : null;
            String estado = (String) body.get("estado");
            String resumo = (String) body.get("resumo");

            Boolean emPromocao = body.get("emPromocao") instanceof Boolean b2 ? b2 : false;
            Double percentualDesconto = body.get("percentualDesconto") != null
                    ? ((Number) body.get("percentualDesconto")).doubleValue() : null;
            String promocaoExpiraStr = (String) body.get("promocaoExpira");
            java.time.LocalDateTime promocaoExpira = null;
            if (promocaoExpiraStr != null && !promocaoExpiraStr.isBlank()) {
                try { promocaoExpira = java.time.LocalDateTime.parse(promocaoExpiraStr); } catch (Exception ignored) {}
            }

            umc.exs.model.enums.EstadoLivro estadoEnum = estado != null
                    ? umc.exs.model.enums.EstadoLivro.valueOf(estado) : null;

            livroService.editarLivroAdmin(id, titulo, autor, isbn, preco, estadoEnum, resumo,
                    emPromocao, percentualDesconto, promocaoExpira);
            return ResponseEntity.ok(Map.of("success", true, "message", "Livro atualizado com sucesso!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    @DeleteMapping("/livros/{id}")
    public ResponseEntity<?> deletarLivro(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("Não autenticado.");
        try {
            livroService.deletarLivroAdmin(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Livro removido do estoque."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
        }
    }

    // ==========================================================
    // CUPONS — Criação promocional
    // ==========================================================

    /** Lista todos os cupons do sistema (mais recentes primeiro). */
    @GetMapping("/cupons")
    public ResponseEntity<List<CupomDTO>> listarTodosCupons() {
        return ResponseEntity.ok(cupomService.listarTodosCupons());
    }

    /** Invalida manualmente um cupom sem creditar tokens. */
    @DeleteMapping("/cupons/{id}")
    public ResponseEntity<?> invalidarCupom(@PathVariable Long id) {
        try {
            cupomService.invalidarCupom(id);
            return ResponseEntity.ok(Map.of("mensagem", "Cupom invalidado com sucesso."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    /**
     * Cria um cupom promocional.
     * Body: percentualDesconto (obrigatório), dataValidade (obrigatório, ISO-8601),
     *       codigo (opcional), quantidadeMaxima (opcional), clienteId (opcional).
     */
    @PostMapping("/cupons")
    public ResponseEntity<?> criarCupomPromocional(@RequestBody Map<String, Object> body) {
        Double percentual = body.get("percentualDesconto") instanceof Number n ? n.doubleValue() : null;
        String dataValidadeStr = body.get("dataValidade") instanceof String s ? s : null;
        String codigoInput = body.get("codigo") instanceof String s ? s : null;
        Integer qtdMaxima = body.get("quantidadeMaxima") instanceof Number n ? n.intValue() : null;
        Long clienteId = body.get("clienteId") instanceof Number n ? n.longValue() : null;

        if (percentual == null || percentual <= 0 || percentual > 100) {
            return ResponseEntity.badRequest().body(Map.of("erro", "percentualDesconto deve estar entre 1 e 100."));
        }
        if (dataValidadeStr == null || dataValidadeStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "dataValidade é obrigatória."));
        }

        java.time.LocalDateTime dataValidade;
        try {
            dataValidade = java.time.LocalDateTime.parse(dataValidadeStr);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Formato de dataValidade inválido. Use ISO-8601 (yyyy-MM-ddTHH:mm:ss)."));
        }

        try {
            Cupom cupom = cupomService.criarCupom(codigoInput, percentual, dataValidade, qtdMaxima, clienteId);
            return ResponseEntity.status(201).body(Map.of(
                    "mensagem", "Cupom criado com sucesso.",
                    "codigo", cupom.getCodigo(),
                    "percentualDesconto", cupom.getPercentualDesconto(),
                    "expiracao", cupom.getExpiracao().toString()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

    // ==========================================================
    // DASHBOARD — Métricas
    // ==========================================================

    /**
     * Retorna todas as métricas da dashboard administrativa:
     * contadores de clientes, livros, visitas, pedidos, tokens
     * e séries mensais dos últimos 12 meses para os gráficos.
     */
    @GetMapping("/dashboard/metricas")
    public ResponseEntity<DashboardMetricasDTO> getMetricas(Model model) {

        model.addAttribute("metrics", dashboardService.getMetricas());
        
        return ResponseEntity.ok(dashboardService.getMetricas());
    }
}
