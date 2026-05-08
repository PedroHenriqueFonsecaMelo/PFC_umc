package umc.exs.controller.api.compras;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.compra.CupomDTO;
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

    /** Resgata um cupom pelo código. */
    @PostMapping("/resgatar")
    public ResponseEntity<?> resgatar(
            @AuthenticationPrincipal UserDetails user,
            @RequestBody Map<String, String> body) {
        if (user == null) return ResponseEntity.status(401).build();

        String codigo = body.get("codigo");
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Código do cupom é obrigatório."));
        }

        try {
            CupomDTO dto = cupomService.resgatarCupom(user.getUsername(), codigo.trim().toUpperCase());
            return ResponseEntity.ok(Map.of(
                    "mensagem", "Cupom resgatado com sucesso! T$ " + dto.getValorTokens() + " creditados.",
                    "cupom", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
        }
    }

}
