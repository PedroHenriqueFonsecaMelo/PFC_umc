package umc.exs.service.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PenalidadeService {

    private final PontuacaoUsuarioRepository pontuacaoRepository;

    private static final int DIAS_GERAR_PENALIDADE = 30;
    private static final int DIAS_PARA_ZERAR = 14;

    @Transactional
    public void aplicarPenalidadeTodos() {

        List<PontuacaoUsuario> pontuacoes = pontuacaoRepository.findAll();

        for (PontuacaoUsuario p : pontuacoes) {
            aplicarPenalidadeInterno(p);
        }

        pontuacaoRepository.saveAll(pontuacoes);
    }

    private void aplicarPenalidadeInterno(PontuacaoUsuario pontuacao) {

        if (pontuacao.getXpTotal() == null || pontuacao.getXpTotal() <= 0) {
            return;
        }

        LocalDateTime ultima = pontuacao.getUltimaAtualizacao();

        if (ultima == null) {
            return;
        }

        long diasDesdeUltimoXp = java.time.temporal.ChronoUnit.DAYS.between(ultima, LocalDateTime.now());

        if (diasDesdeUltimoXp <= DIAS_GERAR_PENALIDADE) {
            return;
        }

        long diasPenalidade = diasDesdeUltimoXp - DIAS_GERAR_PENALIDADE;

        if (diasPenalidade >= DIAS_PARA_ZERAR) {

            pontuacao.setXpTotal(0);
            pontuacao.setXpLivrosAprovados(0);
            pontuacao.setXpCompras(0);
            pontuacao.setXpAvaliacoes(0);

        } else {

            int xpAtual = pontuacao.getXpTotal();

            double fator = (double) (DIAS_PARA_ZERAR - diasPenalidade) / DIAS_PARA_ZERAR;

            int xpNovo = (int) Math.round(xpAtual * fator);

            pontuacao.setXpTotal(xpNovo);

            if (xpAtual > 0) {

                double ratio = (double) xpNovo / xpAtual;

                int xpLivros = pontuacao.getXpLivrosAprovados() != null
                        ? pontuacao.getXpLivrosAprovados()
                        : 0;

                int xpCompras = pontuacao.getXpCompras() != null
                        ? pontuacao.getXpCompras()
                        : 0;

                int xpAvaliacoes = pontuacao.getXpAvaliacoes() != null
                        ? pontuacao.getXpAvaliacoes()
                        : 0;

                pontuacao.setXpLivrosAprovados((int) (xpLivros * ratio));
                pontuacao.setXpCompras((int) (xpCompras * ratio));
                pontuacao.setXpAvaliacoes((int) (xpAvaliacoes * ratio));
            }
        }

    }
}