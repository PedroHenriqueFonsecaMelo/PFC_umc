package umc.exs.controller.api.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.exs.DTOs.gamificacao.MeuPerfilGamificacaoDTO;
import umc.exs.DTOs.gamificacao.RankingItemDTO;
import umc.exs.service.gamificacao.GamificacaoService;

import java.util.List;

/**
 * API REST de gamificação da Bibliotroca.
 *
 * GET /api/gamificacao/ranking → Top 5 global (público)
 * GET /api/gamificacao/meu-perfil → perfil do usuário logado (autenticado)
 */
@RestController
@RequestMapping("/api/gamificacao")
@RequiredArgsConstructor
public class GamificacaoController {

    private final GamificacaoService gamificacaoService;

    /**
     * Retorna o ranking Top 5 global.
     * Endpoint público — usado pelo widget da homepage.
     */
    @GetMapping("/ranking")
    public ResponseEntity<List<RankingItemDTO>> obterRanking() {
        return ResponseEntity.ok(gamificacaoService.obterRankingTop5());
    }

    /**
     * Retorna os dados de gamificação do usuário logado:
     * XP total, nível, badge, posição no ranking e detalhamento por categoria.
     */
    @GetMapping("/meu-perfil")
    public ResponseEntity<?> obterMeuPerfil(@AuthenticationPrincipal UserDetails user) {
        if (user == null) {
            return ResponseEntity.status(401).body("Usuário não autenticado.");
        }
        MeuPerfilGamificacaoDTO perfil = gamificacaoService.obterMeuPerfil(user.getUsername());
        return ResponseEntity.ok(perfil);
    }
}
