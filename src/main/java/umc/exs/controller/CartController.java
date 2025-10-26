package umc.exs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import umc.exs.model.DTO.PagamentoDTO;
import umc.exs.model.compras.Carrinho;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;
import umc.exs.service.CarrinhoService;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CarrinhoService carrinhoService;

    @Autowired
    private ClienteRepository clienteRepository;

    // Adiciona produto ao carrinho
    @PostMapping("/add")
    public ResponseEntity<Carrinho> addProduto(@RequestParam Long produtoId, @RequestParam int quantidade) {
        Carrinho carrinho = carrinhoService.addProduto(produtoId, quantidade);
        return ResponseEntity.ok(carrinho);
    }

    // Remove produto do carrinho
    @PostMapping("/remove")
    public ResponseEntity<Carrinho> removeProduto(@RequestParam Long produtoId) {
        Carrinho carrinho = carrinhoService.removeProduto(produtoId);
        return ResponseEntity.ok(carrinho);
    }

    // Aplica cupom
    @PostMapping("/cupom")
    public ResponseEntity<Carrinho> aplicarCupom(@RequestParam String codigo) {
        Carrinho carrinho = carrinhoService.aplicarCupom(codigo);
        return ResponseEntity.ok(carrinho);
    }

    // Define endereço de entrega
    @PostMapping("/endereco")
    public ResponseEntity<Carrinho> setEndereco(@RequestParam Long enderecoId) {
        Carrinho carrinho = carrinhoService.setEndereco(enderecoId);
        return ResponseEntity.ok(carrinho);
    }

    // Define cartões e valores para pagamento
   @PostMapping("/pagamento")
    public ResponseEntity<?> setPagamentos(@RequestBody List<PagamentoDTO> pagamentos) {
        try {
            Carrinho carrinho = carrinhoService.setPagamentos(pagamentos);
            return ResponseEntity.ok(carrinho);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Finaliza compra
    @PostMapping("/finalizar")
    public ResponseEntity<String> finalizarCompra(@RequestParam Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId).orElse(null);
        if (cliente == null) {
            return ResponseEntity.badRequest().body("Cliente inválido");
        }
        boolean ok = carrinhoService.finalizarCompra(cliente);
        if (ok) return ResponseEntity.ok("Compra finalizada com sucesso!");
        return ResponseEntity.badRequest().body("Erro ao finalizar compra.");
    }

    // Consulta carrinho
    @GetMapping
    public ResponseEntity<Carrinho> getCarrinho() {
        return ResponseEntity.ok(carrinhoService.getCarrinho());
    }
}
