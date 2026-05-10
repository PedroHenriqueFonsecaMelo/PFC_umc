package umc.exs.controller.api.contas;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.admin.AdminAprovacaoDTO;
import umc.exs.dtos.admin.DashboardMetricasDTO;
import umc.exs.dtos.admin.LivroAdminDTO;
import umc.exs.dtos.admin.RejeicaoLivroDTO;
import umc.exs.dtos.compra.AtualizarEnvioDTO;
import umc.exs.dtos.compra.CriarCupomDTO;
import umc.exs.dtos.compra.cupom.CupomDTO;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.dtos.shared.ApiResponseDTO;
import umc.exs.mappers.LivroMapper;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.service.core.bussiness.LivroService;
import umc.exs.service.core.control.DashboardService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.cupom.CupomService;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminControllerApi {

    private final LivroService livroService;
    private final AdminRepository adminRepository;
    private final PedidoService pedidoService;
    private final DashboardService dashboardService;
    private final CupomService cupomService;

    private final LivroMapper livroMapper;

    private static final String NAO_AUTENTICADO = "Não autenticado.";
    private static final String ADMIN_NAO_ENCONTRADO = "Admin não encontrado.";
    private static final String ADMIN_NULO = "ID do administrador não pode ser nulo";

    // ==========================================================
    // LOTES
    // ==========================================================

    @GetMapping("/lotes/pendentes")
    public ResponseEntity<List<LivroDTO>> listarLotesPendentes() {
        return ResponseEntity.ok(livroService.listarLivrosPendentes());
    }

    @GetMapping("/lotes/{id}")
    public ResponseEntity<List<LivroDTO>> listarLivrosLote(@PathVariable Long id) {
        return ResponseEntity.ok(livroService.listarLivrosPorLote(id));
    }

    // ==========================================================
    // LIVROS
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

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        Administrador admin = adminRepository.findByEmail(user.getUsername())
                .orElse(null);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, ADMIN_NAO_ENCONTRADO));
        }

        // Correção de Null Safety: garantindo que o ID do admin não é nulo
        Long adminId = Objects.requireNonNull(admin.getId(), ADMIN_NULO);
        livroService.aprovarLivro(id, adminId, dto);

        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro aprovado com sucesso"));
    }

    @PostMapping("/livros/{id}/rejeitar")
    public ResponseEntity<ApiResponseDTO> rejeitarLivro(
            @PathVariable Long id,
            @RequestBody RejeicaoLivroDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        Administrador admin = adminRepository.findByEmail(user.getUsername()).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, ADMIN_NAO_ENCONTRADO));
        }

        Long adminId = Objects.requireNonNull(admin.getId(), ADMIN_NULO);
        livroService.rejeitarLivro(id, adminId, dto.getEstado(), dto.getComentario());

        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro rejeitado"));
    }

    @GetMapping("/livros/aprovados")
    public ResponseEntity<List<LivroDTO>> listarLivrosAprovados() {
        return ResponseEntity.ok(livroService.listarLivrosAprovados());
    }

    @PostMapping("/livros/novo")
    public ResponseEntity<ApiResponseDTO> adicionarLivro(
            @RequestBody LivroAdminDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        Administrador admin = adminRepository.findByEmail(user.getUsername()).orElse(null);
        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, ADMIN_NAO_ENCONTRADO));
        }

        Long adminId = Objects.requireNonNull(admin.getId(), ADMIN_NULO);
        LivroDTO livro = livroService.adicionarLivroAdmin(
                dto.getTitulo(),
                dto.getAutor(),
                dto.getIsbn(),
                dto.getPreco(),
                EstadoLivro.valueOf(dto.getEstado()),
                dto.getResumo(),
                adminId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponseDTO(true, "Livro criado: " + livro.getId()));
    }

    @PutMapping("/livros/{id}")
    public ResponseEntity<ApiResponseDTO> editarLivro(
            @PathVariable Long id,
            @RequestBody LivroAdminDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        livroService.editarLivroAdmin(
                id,
                dto.getTitulo(),
                dto.getAutor(),
                dto.getIsbn(),
                dto.getPreco(),
                dto.getEstado() != null ? EstadoLivro.valueOf(dto.getEstado()) : null,
                dto.getResumo());

        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro atualizado"));
    }

    @DeleteMapping("/livros/{id}")
    public ResponseEntity<ApiResponseDTO> deletarLivro(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        livroService.deletarLivroAdmin(id);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Livro removido"));
    }

    // ==========================================================
    // PEDIDOS
    // ==========================================================

    @GetMapping("/pedidos")
    public ResponseEntity<List<?>> listarPedidos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @PostMapping("/pedidos/{id}/envio")
    public ResponseEntity<Object> atualizarEnvio(
            @PathVariable Long id,
            @RequestBody AtualizarEnvioDTO dto,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO(false, NAO_AUTENTICADO));
        }

        return ResponseEntity.ok(
                pedidoService.atualizarStatus(id, dto.getStatusEnvio(), dto.getCodigoRastreio()));
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
        cupomService.invalidarCupom(id);
        return ResponseEntity.ok(new ApiResponseDTO(true, "Cupom invalidado"));
    }

    @PostMapping("/cupons")
    public ResponseEntity<?> criarCupom(@RequestBody CriarCupomDTO dto) {
        try {
            // iFood geralmente usa datas no formato ISO (2026-05-30T23:59:59)
            LocalDateTime data = LocalDateTime.parse(dto.getDataValidade());

            Cupom cupom = cupomService.criarCupom(dto, data);

            return ResponseEntity.status(HttpStatus.CREATED).body(cupom);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Formato de data inválido. Use ISO-8601.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    // ==========================================================
    // DASHBOARD
    // ==========================================================

    @GetMapping("/dashboard/metricas")
    public ResponseEntity<DashboardMetricasDTO> getMetricas() {
        return ResponseEntity.ok(dashboardService.getMetricas());
    }
}