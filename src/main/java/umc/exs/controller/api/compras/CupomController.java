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

    /**
     * Valida um cupom para um item específico do carrinho (preview do desconto).
     * Não registra uso — apenas calcula o desconto.
     *
     * @param codigo  código do cupom
     * @param livroId ID do livro ao qual o desconto seria aplicado
     * @param user    usuário autenticado
     * @return mapa com: valido, percentual, precoOriginal, precoComDesconto, economia, mensagem
     */
    @GetMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(
            @RequestParam String codigo,
            @RequestParam Long livroId,
            @AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).build();
        Map<String, Object> result = cupomService.validarCupom(codigo, user.getUsername(), livroId);
        return ResponseEntity.ok(result);
    }
}
