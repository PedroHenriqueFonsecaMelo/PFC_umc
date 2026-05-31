package umc.exs.service.core.livros.recompensa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.gamificacao.GamificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroRecompensaService {

    /** Mantém o valor existente no fluxo atual. */
    public static final double TOKEN_REWARD = 10.0;

    private final GamificacaoService gamificacaoService;

    /**
     * Mantém responsabilidade apenas de XP.
     * O ajuste de tokens (saldo) deve ficar com o serviço transacional (ex:
     * aprovação).
     */
    public void recompensarVendedorPorAprovacao(Cliente vendedor) {
        if (vendedor == null) {
            return;
        }

        try {
            gamificacaoService.xpLivroAprovado(vendedor.getId());
        } catch (Exception e) {
            // Side-effect não pode quebrar aprovação.
            log.error("Erro gamificação ao recompensar vendedor {}: {}", vendedor.getEmail(), e.getMessage(), e);
        }
    }
}
