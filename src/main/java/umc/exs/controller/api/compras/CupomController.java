package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.dtos.compra.cupom.CupomDTO;
import umc.exs.service.cupom.CupomService;

@RestController
@RequestMapping("/api/cupons")
@RequiredArgsConstructor
public class CupomController {

    private final CupomService cupomService;

    /** Lista os cupons disponíveis do usuário logado. */
    @GetMapping("/meus")
    public ResponseEntity<List<CupomDTO>> meusCupons(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(cupomService.listarCuponsDisponiveis(user.getUsername()));
    }

    /**
     * Valida um cupom sobre o total do carrinho (preview do desconto).
     * Não registra uso — apenas calcula o desconto.
     *
     * @param codigo código do cupom
     * @param total  valor total da compra sobre o qual aplicar o desconto
     * @param user   usuário autenticado
     * @return mapa com: valido, codigo, percentual, totalOriginal, desconto, totalComDesconto
     */
    @GetMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(
            @RequestParam String codigo,
            @RequestParam double total,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null) return ResponseEntity.status(401).build();

        try {
            var result = cupomService.validarCupomParaTotal(codigo, user.getUsername(), total);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> err = new java.util.LinkedHashMap<>();
            err.put("valido", false);
            err.put("mensagem", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

}
