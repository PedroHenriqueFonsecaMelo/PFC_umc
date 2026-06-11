package umc.exs.service.core.livros.recompensa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.gamificacao.GamificacaoService;

/**
 * Serviço responsável por recompensar o vendedor com XP quando um livro é aprovado.
 * Mantém a lógica de gamificação isolada do fluxo principal de aprovação.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LivroRecompensaService {

    /** Valor de tokens concedidos como recompensa por aprovação de livro. */
    public static final double TOKEN_REWARD = 10.0;

    private final GamificacaoService gamificacaoService;

    /**
     * Concede XP ao vendedor quando seu livro é aprovado pelo administrador.
     * Erros na gamificação são apenas logados e não interrompem o fluxo de aprovação.
     */
    public void recompensarVendedorPorAprovacao(Cliente vendedor) {
        if (vendedor == null) {
            return;
        }

        try {
            gamificacaoService.xpLivroAprovado(vendedor.getId());
        } catch (Exception e) {
            // Gamificação é um efeito colateral; falhas não devem bloquear a aprovação do livro
            log.error("Erro gamificação ao recompensar vendedor {}: {}", vendedor.getEmail(), e.getMessage(), e);
        }
    }
}
