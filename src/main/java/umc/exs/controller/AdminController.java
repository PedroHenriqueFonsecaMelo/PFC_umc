package umc.exs.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import umc.exs.model.compras.Cupom;
import umc.exs.model.compras.Pedido;
import umc.exs.model.compras.PedidoItem;
import umc.exs.model.compras.Troca;
import umc.exs.model.foundation.Admin;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.AdminRepository;
import umc.exs.repository.CupomRepository;
import umc.exs.repository.PedidoItemRepository;
import umc.exs.repository.PedidoRepository;
import umc.exs.repository.ProdutoRepository;
import umc.exs.repository.TrocaRepository;
import umc.exs.service.SalesService;

@RestController
@RequestMapping("/admin")
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
    private PedidoRepository pedidoRepository;

    @Autowired
    private PedidoItemRepository pedidoItemRepository;

    @Autowired
    private SalesService salesService;

    // ---------- Admin CRUD ----------
    @PostMapping("/admins")
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        Admin saved = adminRepository.save(admin);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admins")
    public ResponseEntity<List<Admin>> listAdmins() {
        return ResponseEntity.ok(adminRepository.findAll());
    }

    @GetMapping("/admins/{id}")
    public ResponseEntity<Admin> getAdmin(@PathVariable Long id) {
        return adminRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/admins/{id}")
    public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        return adminRepository.findById(id).map(existing -> {
            existing.setNome(admin.getNome());
            existing.setEmail(admin.getEmail());
            // atualizar outros campos necessários (não sobrescrever senha sem hash)
            adminRepository.save(existing);
            return ResponseEntity.ok(existing);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/admins/{id}")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        if (!adminRepository.existsById(id))
            return ResponseEntity.notFound().build();
        adminRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Produtos (admins podem criar/excluir) ----------
    @PostMapping("/products")
    public ResponseEntity<Produto> createProduct(@RequestBody Produto produto) {
        Produto saved = produtoRepository.save(produto);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (!produtoRepository.existsById(id))
            return ResponseEntity.notFound().build();
        produtoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Troca/Devolução (aprovar / reprovar) ----------
    @PostMapping("/exchanges/{id}/approve")
    public ResponseEntity<?> approveExchange(@PathVariable Long id) {
        Optional<Troca> trocaOpt = trocaRepository.findById(id);
        if (trocaOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Troca troca = trocaOpt.get();
        if ("APPROVED".equalsIgnoreCase(troca.getStatus())) {
            return ResponseEntity.badRequest().body("Troca já aprovada");
        }

        // marcar como aprovado
        troca.setStatus("APPROVED");
        troca.setDecisaoPor("admin");
        troca.setDecisionAt(LocalDateTime.now());
        trocaRepository.save(troca);

        double valor = troca.getValor() <= 0 ? computeOrderTotalForTroca(troca) : troca.getValor();
        Cupom cupom = new Cupom();
        cupom.setCodigo("CPN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        cupom.setValor((float) valor);
        cupom.setClienteId(troca.getClienteId());
        cupom.setExpiracao(LocalDateTime.now().plusMonths(3));
        cupomRepository.save(cupom);

        return ResponseEntity.ok(Map.of("troca", troca.getId(), "cupom", cupom.getCodigo(), "valor", cupom.getValor()));
    }

    @PostMapping("/exchanges/{id}/reject")
    public ResponseEntity<?> rejectExchange(@PathVariable Long id, @RequestParam(required = false) String motivo) {
        Optional<Troca> trocaOpt = trocaRepository.findById(id);
        if (trocaOpt.isEmpty())
            return ResponseEntity.notFound().build();

        Troca troca = trocaOpt.get();
        troca.setStatus("REJECTED");
        troca.setMotivoRejeicao(motivo);
        troca.setDecisaoPor("admin");
        troca.setDecisionAt(LocalDateTime.now());
        trocaRepository.save(troca);

        return ResponseEntity.ok(Map.of("troca", troca.getId(), "status", troca.getStatus()));
    }

    private double computeOrderTotalForTroca(Troca troca) {
        if (troca.getPedidoId() == null)
            return 0.0;
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(troca.getPedidoId());
        if (pedidoOpt.isEmpty())
            return 0.0;
        Pedido pedido = pedidoOpt.get();
        // se Pedido possui total:
        if (pedido.getTotal() != null)
            return pedido.getTotal();
        // caso contrário soma itens
        List<PedidoItem> items = pedidoItemRepository.findByPedidoId(pedido.getId());
        return items.stream().mapToDouble(it -> it.getQuantidade() * it.getPrecoUnitario()).sum();
    }

    @GetMapping("/sales/stats")
    public ResponseEntity<Map<String, Object>> salesStats(
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until) {

        Map<String, Object> stats = salesService.computeSalesStats(since, until);
        return ResponseEntity.ok(stats);
    }
}