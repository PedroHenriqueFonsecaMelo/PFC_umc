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
import umc.exs.service.log.LogAuditoriaService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço central de gamificação da Bibliotroca.
 *
 * Responsabilidades:
 * - Criar/atualizar pontuação do usuário
 * - Calcular nível e badge com base em XP
 * - Retornar ranking Top 5 global
 * - Retornar perfil de gamificação do usuário logado
 *
 * XP concedido por ação:
 * - Livro aprovado pelo admin : +50 XP (categoria APROVACAO)
 * - Comprar um livro : +30 XP (categoria COMPRA)
 * - Avaliar um livro : +10 XP (categoria AVALIACAO)
 */
@Service
@RequiredArgsConstructor
public class GamificacaoService {

    // --- Constantes de XP ---
    public static final int XP_LIVRO_APROVADO = 50;
    public static final int XP_COMPRA = 30;
    public static final int XP_AVALIACAO = 10;

    private final PontuacaoUsuarioRepository pontuacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    // -------------------------------------------------------
    // ADICIONAR XP
    // -------------------------------------------------------

    /**
     * Ponto de entrada genérico para adicionar XP a um cliente.
     * Cria o registro de pontuação automaticamente se ainda não existir.
     *
     * @param clienteId ID do cliente
     * @param xp        quantidade de XP a adicionar
     * @param categoria "APROVACAO" | "COMPRA" | "AVALIACAO"
     */
    @Transactional
    public void adicionarXp(Long clienteId, int xp, String categoria) {
        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteId(clienteId)
                .orElseGet(() -> criarPontuacaoInicial(clienteId));

        int xpAntes = pontuacao.getXpTotal();
        pontuacao.adicionarXp(xp, categoria);
        pontuacaoRepository.save(pontuacao);

        // Log se subiu de nível
        NivelUsuario nivelAntes = NivelUsuario.calcular(xpAntes);
        NivelUsuario nivelDepois = NivelUsuario.calcular(pontuacao.getXpTotal());
        if (!nivelAntes.equals(nivelDepois)) {
            logAuditoria.registrarLog(
                    "NIVEL_SUBIU",
                    clienteId,
                    pontuacao.getCliente().getEmail(),
                    "Subiu para " + nivelDepois.getDescricao() + " (" + pontuacao.getXpTotal() + " XP)");
        }
    }

    /** Atalho para livro aprovado */
    @Transactional
    public void xpLivroAprovado(Long clienteId) {
        adicionarXp(clienteId, XP_LIVRO_APROVADO, "APROVACAO");
    }

    /** Atalho para compra de livro */
    @Transactional
    public void xpCompra(Long clienteId) {
        adicionarXp(clienteId, XP_COMPRA, "COMPRA");
    }

    /** Atalho para avaliação de livro */
    @Transactional
    public void xpAvaliacao(Long clienteId) {
        adicionarXp(clienteId, XP_AVALIACAO, "AVALIACAO");
    }

    // -------------------------------------------------------
    // RANKING
    // -------------------------------------------------------

    /**
     * Retorna o Top 5 global ordenado por XP decrescente.
     */
    public List<RankingItemDTO> obterRankingTop5() {
        List<PontuacaoUsuario> top = pontuacaoRepository.findTopByOrderByXpTotalDesc(PageRequest.of(0, 5));
        List<RankingItemDTO> ranking = new ArrayList<>();

        for (int i = 0; i < top.size(); i++) {
            PontuacaoUsuario p = top.get(i);
            NivelUsuario nivel = p.getNivel();

            ranking.add(new RankingItemDTO(
                    i + 1,
                    p.getCliente().getNome(),
                    p.getXpTotal(),
                    nivel.getDescricao(),
                    nivel.getBadge(),
                    p.getXpLivrosAprovados(),
                    p.getXpCompras(),
                    p.getXpAvaliacoes()));
        }
        return ranking;
    }

    // -------------------------------------------------------
    // PERFIL DO USUÁRIO LOGADO
    // -------------------------------------------------------

    /**
     * Retorna os dados de gamificação do usuário logado
     * (XP, nível, badge, posição no ranking, XP faltando).
     */
    public MeuPerfilGamificacaoDTO obterMeuPerfil(String email) {
        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteEmail(email)
                .orElse(null);

        if (pontuacao == null) {
            // Usuário ainda sem XP
            return new MeuPerfilGamificacaoDTO(
                    email, 0,
                    NivelUsuario.INICIANTE.getDescricao(),
                    NivelUsuario.INICIANTE.getBadge(),
                    NivelUsuario.BRONZE.getXpMinimo(), // 200 XP para o próximo
                    0, 0, 0, 0);
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

    // -------------------------------------------------------
    // HELPERS PRIVADOS
    // -------------------------------------------------------

    @SuppressWarnings("null")
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
                .build();
    }

    private int calcularXpParaProximoNivel(NivelUsuario nivelAtual, int xpAtual) {
        NivelUsuario[] niveis = NivelUsuario.values();
        for (int i = 0; i < niveis.length - 1; i++) {
            if (niveis[i] == nivelAtual) {
                return niveis[i + 1].getXpMinimo() - xpAtual;
            }
        }
        return 0; // já está no nível máximo
    }

    private int calcularPosicaoRanking(Long clienteId) {
        PontuacaoUsuario pontuacao = pontuacaoRepository.findByClienteId(clienteId).orElse(null);
        if (pontuacao == null) {
            return (int) pontuacaoRepository.count() + 1;
        }
        long acima = pontuacaoRepository.countByXpTotalGreaterThan(pontuacao.getXpTotal());
        return (int) acima + 1;
    }
}
