package umc.exs.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import umc.exs.backstage.service.SalesService;
import umc.exs.model.DAO.AdminMapper;
import umc.exs.model.DAO.ProdutoMapper;
import umc.exs.model.DAO.TrocaMapper;
import umc.exs.model.DTO.AdminDTO;
import umc.exs.model.DTO.ProdutoDTO;
import umc.exs.model.DTO.TrocaDTO;
import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.Troca;
import umc.exs.model.foundation.Administrador;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.AdminRepository;
import umc.exs.repository.CupomRepository;
import umc.exs.repository.ProdutoRepository;
import umc.exs.repository.TrocaRepository;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private TrocaRepository trocaRepository;

    @Autowired
    private CupomRepository cupomRepository;

    @Autowired
    private SalesService salesService;

    // Admin CRUD usando DTOs
    @PostMapping("/admins")
    public ResponseEntity<AdminDTO> createAdmin(@RequestBody AdminDTO adminDto) {
        Administrador admin = AdminMapper.toEntity(adminDto);
        Administrador saved = adminRepository.save(admin);
        return ResponseEntity.ok(AdminMapper.fromEntity(saved));
    }

    @GetMapping("/admins")
    public ResponseEntity<List<AdminDTO>> listAdmins() {
        List<AdminDTO> dtos = adminRepository.findAll().stream().map(AdminMapper::fromEntity).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/admins/{id}")
    public ResponseEntity<AdminDTO> getAdmin(@PathVariable Long id) {
        return adminRepository.findById(id).map(AdminMapper::fromEntity).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<AdminDTO> updateAdmin(@PathVariable Long id, @RequestBody AdminDTO adminDto) {
        return adminRepository.findById(id).map(existing -> {
            existing.setNome(adminDto.getNome());
            existing.setEmail(adminDto.getEmail());

            if (adminDto.getPassword() != null && !adminDto.getPassword().isBlank()) {
                existing.setPassword(adminDto.getPassword());
            }

            Administrador updated = adminRepository.save(existing);
            return ResponseEntity.ok(AdminDTO.fromEntity(updated));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        if (!adminRepository.existsById(id))
            return ResponseEntity.notFound().build();
        adminRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Produtos com DTOs
    @PostMapping("/products")
    public ResponseEntity<ProdutoDTO> createProduct(@RequestBody ProdutoDTO produtoDto) {
        Produto produto = ProdutoMapper.toEntity(produtoDto);
        Produto saved = produtoRepository.save(produto);
        return ResponseEntity.ok(ProdutoMapper.fromEntity(saved));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!produtoRepository.existsById(id))
            return ResponseEntity.notFound().build();
        produtoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Aprovar / rejeitar troca - retorna DTOs
    @PostMapping("/exchanges/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveExchange(@PathVariable Long id) {
        Optional<Troca> trocaOpt = trocaRepository.findById(id);
        if (trocaOpt.isEmpty()) return ResponseEntity.notFound().build();

        Troca troca = trocaOpt.get();
        if ("APPROVED".equalsIgnoreCase(troca.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Troca já aprovada"));
        }

        troca.setStatus("APPROVED");
        troca.setDecisaoPor("admin");
        troca.setDecisionAt(LocalDateTime.now());
        trocaRepository.save(troca);

        if (troca.getValor() == null || troca.getValor() <= 0) {
            troca.setValor(0.0);
        }

        double valor = troca.getValor() != null && troca.getValor() > 0 ? troca.getValor() : 0.0;
        Cupom cupom = new Cupom();
        cupom.setCodigo("CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cupom.setValor((float) valor);
        cupom.setClienteId(troca.getClienteId());
        cupom.setExpiracao(LocalDateTime.now().plusMonths(3));
        cupomRepository.save(cupom);

        Map<String, Object> resp = new HashMap<>();
        resp.put("troca", TrocaMapper.fromEntity(troca));
        // retornar apenas o código do cupom (String) para compatibilidade com o teste
        resp.put("cupom", cupom.getCodigo());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/exchanges/{id}/reject")
    public ResponseEntity<TrocaDTO> rejectExchange(@PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        Optional<Troca> trocaOpt = trocaRepository.findById(id);
        if (trocaOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Troca troca = trocaOpt.get();
        troca.setStatus("REJECTED");
        troca.setMotivoRejeicao(motivo);
        troca.setDecisaoPor("admin");
        troca.setDecisionAt(LocalDateTime.now());
        trocaRepository.save(troca);

        return ResponseEntity.ok(TrocaMapper.fromEntity(troca));
    }

    // Estatísticas de vendas (mantém privacidade) - delega service
    @GetMapping("/sales/stats")
    public ResponseEntity<Map<String, Object>> salesStats(
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {
        Map<String, Object> stats = salesService.computeSalesStats(since, until);
        return ResponseEntity.ok(stats);
    }
}