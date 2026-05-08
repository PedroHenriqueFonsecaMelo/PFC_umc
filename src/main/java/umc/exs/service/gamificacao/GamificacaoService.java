package umc.exs.service.gamificacao;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import umc.exs.DTOs.gamificacao.MeuPerfilGamificacaoDTO;
import umc.exs.DTOs.gamificacao.RankingItemDTO;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.NivelUsuario;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.log.LogAuditoriaService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificacaoService {

    private static final int XP_LIVRO_APROVADO = 50;
    private static final int XP_COMPRA = 30;
    private static final int XP_AVALIACAO = 10;
    private static final int XP_THRESHOLD_CUPOM = 500;

    private static final int DIAS_SEM_XP = 30;
    private static final int DIAS_PARA_ZERAR = 15;

    private final PontuacaoUsuarioRepository pontuacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;
    private final CupomService cupomService;

    // ---------------- XP ----------------

    @Transactional
    public void adicionarXp(Long clienteId, int xp, String categoria) {

        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteId(clienteId)
                .orElseGet(() -> criarPontuacaoInicial(clienteId));

        int xpAntes = pontuacao.getXpTotal();

        pontuacao.adicionarXp(xp, categoria);
        pontuacao.setUltimaAtualizacao(LocalDateTime.now());

        pontuacaoRepository.save(pontuacao);

        int xpDepois = pontuacao.getXpTotal();

        int thresholdAntes = xpAntes / XP_THRESHOLD_CUPOM;
        int thresholdDepois = xpDepois / XP_THRESHOLD_CUPOM;

        if (thresholdDepois > thresholdAntes) {
            try {
                cupomService.gerarCupomPorPontuacao(clienteId);
            } catch (Exception e) {
                logAuditoria.registrarLog(
                        "ERRO_CUPOM",
                        clienteId,
                        null,
                        "Erro ao gerar cupom: " + e.getMessage());
            }
        }

        NivelUsuario nivelAntes = NivelUsuario.calcular(xpAntes);
        NivelUsuario nivelDepois = NivelUsuario.calcular(xpDepois);

        if (!nivelAntes.equals(nivelDepois)) {
            logAuditoria.registrarLog(
                    "NIVEL_SUBIU",
                    clienteId,
                    pontuacao.getCliente().getEmail(),
                    "Subiu para " + nivelDepois.getDescricao() + " (" + xpDepois + " XP)");
        }
    }

    // ---------------- PENALIDADE POR INATIVIDADE ----------------

    public void aplicarPenalidadeXpExpirada(String email) {
        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteEmail(email)
                .orElse(null);

        aplicarPenalidadeXpExpirada(pontuacao);
    }

    @Transactional
    public void aplicarPenalidadeXpExpirada(PontuacaoUsuario pontuacao) {

        if (pontuacao == null || pontuacao.getUltimaAtualizacao() == null) {
            return;
        }

        long diasSemXp = ChronoUnit.DAYS.between(
                pontuacao.getUltimaAtualizacao(),
                LocalDateTime.now());

        if (diasSemXp <= DIAS_SEM_XP) {
            return;
        }

        long diasPenalidade = diasSemXp - DIAS_SEM_XP;

        int xpAntes = pontuacao.getXpTotal();

        if (diasPenalidade >= DIAS_PARA_ZERAR) {

            pontuacao.setXpTotal(0);
            pontuacao.setXpLivrosAprovados(0);
            pontuacao.setXpCompras(0);
            pontuacao.setXpAvaliacoes(0);

            logAuditoria.registrarLog(
                    "XP_ZERADO",
                    pontuacao.getCliente().getId(),
                    pontuacao.getCliente().getEmail(),
                    "XP zerado por inatividade");

        } else {

            double fator = (double) (DIAS_PARA_ZERAR - diasPenalidade) / DIAS_PARA_ZERAR;

            int xpNovo = (int) Math.round(pontuacao.getXpTotal() * fator);
            xpNovo = Math.max(0, xpNovo);

            pontuacao.setXpTotal(xpNovo);

            if (xpAntes > 0) {
                double ratio = (double) xpNovo / xpAntes;

                pontuacao.setXpLivrosAprovados((int) (pontuacao.getXpLivrosAprovados() * ratio));
                pontuacao.setXpCompras((int) (pontuacao.getXpCompras() * ratio));
                pontuacao.setXpAvaliacoes((int) (pontuacao.getXpAvaliacoes() * ratio));
            }

            logAuditoria.registrarLog(
                    "XP_REDUZIDO",
                    pontuacao.getCliente().getId(),
                    pontuacao.getCliente().getEmail(),
                    "XP reduzido por inatividade");
        }

        pontuacaoRepository.save(pontuacao);
    }

    // ---------------- BUSCA ----------------

    public PontuacaoUsuario buscarPontuacaoPorEmail(String email) {
        return pontuacaoRepository.findByClienteEmail(email).orElse(null);
    }

    // ---------------- XP POR AÇÃO ----------------

    @Transactional
    public void xpLivroAprovado(Long clienteId) {
        adicionarXp(clienteId, XP_LIVRO_APROVADO, "APROVACAO");
    }

    @Transactional
    public void xpCompra(Long clienteId) {
        adicionarXp(clienteId, XP_COMPRA, "COMPRA");
    }

    @Transactional
    public void xpAvaliacao(Long clienteId) {
        adicionarXp(clienteId, XP_AVALIACAO, "AVALIACAO");
    }

    // ---------------- RANKING ----------------

    public List<RankingItemDTO> obterRankingTop5() {

        List<PontuacaoUsuario> top = pontuacaoRepository
                .findTopByOrderByXpTotalDesc(PageRequest.of(0, 5));

        List<RankingItemDTO> ranking = new ArrayList<>();

        for (int i = 0; i < top.size(); i++) {

            PontuacaoUsuario p = top.get(i);
            NivelUsuario nivel = p.getNivel();

            String nome = p.getCliente() != null ? p.getCliente().getNome() : "Desconhecido";

            ranking.add(new RankingItemDTO(
                    i + 1,
                    nome,
                    p.getXpTotal(),
                    nivel.getDescricao(),
                    nivel.getBadge(),
                    p.getXpLivrosAprovados(),
                    p.getXpCompras(),
                    p.getXpAvaliacoes()));
        }

        return ranking;
    }

    // ---------------- PERFIL ----------------

    public MeuPerfilGamificacaoDTO obterMeuPerfil(String email) {

        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteEmail(email)
                .orElse(null);

        aplicarPenalidadeXpExpirada(pontuacao);

        if (pontuacao == null) {
            return new MeuPerfilGamificacaoDTO(
                    email,
                    0,
                    NivelUsuario.INICIANTE.getDescricao(),
                    NivelUsuario.INICIANTE.getBadge(),
                    NivelUsuario.BRONZE.getXpMinimo(),
                    0,
                    0,
                    0,
                    0);
        }

        NivelUsuario nivel = pontuacao.getNivel();

        int xpProximo = calcularXpParaProximoNivel(nivel, pontuacao.getXpTotal());
        int posicao = calcularPosicaoRanking(pontuacao.getCliente().getId());

        return new MeuPerfilGamificacaoDTO(
                pontuacao.getCliente().getNome(),
                pontuacao.getXpTotal(),
                nivel.getDescricao(),
                nivel.getBadge(),
                xpProximo,
                posicao,
                pontuacao.getXpLivrosAprovados(),
                pontuacao.getXpCompras(),
                pontuacao.getXpAvaliacoes());
    }

    // ---------------- HELPERS ----------------

    private PontuacaoUsuario criarPontuacaoInicial(Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));

        return PontuacaoUsuario.builder()
                .cliente(cliente)
                .xpTotal(0)
                .xpLivrosAprovados(0)
                .xpCompras(0)
                .xpAvaliacoes(0)
                .ultimaAtualizacao(LocalDateTime.now())
                .dataExpiracao(LocalDateTime.now().plusDays(30))
                .build();
    }

    private int calcularXpParaProximoNivel(NivelUsuario nivelAtual, int xpAtual) {

        NivelUsuario[] niveis = NivelUsuario.values();

        for (int i = 0; i < niveis.length - 1; i++) {
            if (niveis[i] == nivelAtual) {
                return niveis[i + 1].getXpMinimo() - xpAtual;
            }
        }

        return 0;
    }

    private int calcularPosicaoRanking(Long clienteId) {

        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteId(clienteId)
                .orElse(null);

        if (pontuacao == null) {
            return (int) pontuacaoRepository.count() + 1;
        }

        long acima = pontuacaoRepository
                .countByXpTotalGreaterThan(pontuacao.getXpTotal());

        return (int) acima + 1;
    }
}