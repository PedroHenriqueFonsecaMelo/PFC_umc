package umc.exs.controller.api.interaction;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.gamificacao.RankingPublicoDTO;
import umc.exs.service.gamificacao.GamificacaoService;

/**
 * API pública de ranking da Bibliotroca.
 *
 * GET /api/ranking/top?limite=100&periodo=todos  → top N leitores (LGPD-compliant)
 *
 * Acesso: público (sem autenticação). Dados sensíveis (e-mail, CPF) nunca expostos.
 */
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final GamificacaoService gamificacaoService;

    /**
     * Retorna o ranking público de leitores.
     *
     * @param limite  número de posições desejado (1–100; padrão 100)
     * @param periodo "mes", "ano" ou "todos" (padrão "todos")
     */
    @GetMapping("/top")
    public ResponseEntity<List<RankingPublicoDTO>> top(
            @RequestParam(defaultValue = "100") int limite,
            @RequestParam(defaultValue = "todos") String periodo) {

        return ResponseEntity.ok(gamificacaoService.obterRankingPublico(limite, periodo));
    }
}
