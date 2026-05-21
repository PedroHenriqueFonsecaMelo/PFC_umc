package umc.exs.service.gamificacao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import umc.exs.dto.response.gamificacao.MeuPerfilGameResponse;
import umc.exs.dto.response.gamificacao.RankingDetalhadoResponse;
import umc.exs.dto.response.gamificacao.RankingPublicResponse;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.NivelUsuario;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificacaoService {

    private static final int XP_LIVRO_APROVADO = 50;
    private static final int XP_COMPRA = 30;
    private static final int XP_AVALIACAO = 10;
    private static final int XP_THRESHOLD_CUPOM = 500;

    private final PontuacaoUsuarioRepository pontuacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;
    private final CupomService cupomService;
    private final NotificacaoService notificacaoService;

    // Resolução do Sonar java:S6809: Auto-injeção para chamadas transacionais
    private GamificacaoService self;

    @Autowired
    public void setSelf(@Lazy GamificacaoService self) {
        this.self = self;
    }

    // ==========================================
    // XP (GANHOS E TRANSAÇÕES)
    // ==========================================

    @Transactional
    public void adicionarXp(Long clienteId, int xp, String categoria) {
        Objects.requireNonNull(clienteId, "clienteId não pode ser null");

        PontuacaoUsuario pontuacao = pontuacaoRepository
                .findByClienteId(clienteId)
                .orElseGet(() -> criarPontuacaoInicial(clienteId));

        int xpAntes = pontuacao.getXpTotal();

        // Adiciona XP e renova a data para impedir o decay automático
        pontuacao.adicionarXp(xp, categoria);
        pontuacao.setUltimaAtualizacao(LocalDateTime.now());

        pontuacaoRepository.save(pontuacao);

        processarRegrasPosGanho(clienteId, pontuacao, xpAntes);
    }

    private void processarRegrasPosGanho(@NonNull Long clienteId, PontuacaoUsuario pontuacao, int xpAntes) {
        int xpDepois = pontuacao.getXpTotal();

        // Regra de Cupom
        if ((xpDepois / XP_THRESHOLD_CUPOM) > (xpAntes / XP_THRESHOLD_CUPOM)) {
            try {
                cupomService.gerarCupomPorPontuacao(clienteId);
            } catch (Exception e) {
                logAuditoria.registrarLog("ERRO_CUPOM", clienteId, null, "Erro: " + e.getMessage());
            }
        }

        // Regra de Nível
        NivelUsuario nivelAntes = NivelUsuario.calcular(xpAntes);
        NivelUsuario nivelDepois = NivelUsuario.calcular(xpDepois);

        if (!nivelAntes.equals(nivelDepois)) {
            logAuditoria.registrarLog("NIVEL_SUBIU", clienteId,
                pontuacao.getCliente().getEmail(), "Subiu para " + nivelDepois.getDescricao());

            // Notificação dashboard: novo nível atingido
            try {
                notificacaoService.criarNotificacaoDashboard(
                        pontuacao.getCliente(),
                        String.format("Parabéns! Você atingiu o nível %s %s no ranking!",
                                nivelDepois.getBadge(), nivelDepois.getDescricao()),
                        "/gamificacao");
            } catch (Exception e) {
                logAuditoria.registrarLog("ERRO_NOTIF_NIVEL", clienteId, null, "Erro: " + e.getMessage());
            }
        }
    }

    public void xpLivroAprovado(Long clienteId) {
        self.adicionarXp(clienteId, XP_LIVRO_APROVADO, "LIVRO_APROVADO");
    }

    public void xpCompra(Long clienteId) {
        self.adicionarXp(clienteId, XP_COMPRA, "COMPRA");
    }

    public void xpAvaliacao(Long clienteId) {
        self.adicionarXp(clienteId, XP_AVALIACAO, "AVALIACAO");
    }

    // ==========================================
    // CONSULTAS E RANKING
    // ==========================================

    public PontuacaoUsuario buscarPontuacaoPorEmail(String email) {
        return pontuacaoRepository.findByClienteEmail(email).orElse(null);
    }

    public MeuPerfilGameResponse obterMeuPerfil(String email) {
        PontuacaoUsuario pontuacao = buscarPontuacaoPorEmail(email);

        if (pontuacao == null) {
            return criarPerfilVazio(email);
        }

        NivelUsuario nivel = pontuacao.getNivel();
        int xpProximo = calcularXpParaProximoNivel(nivel, pontuacao.getXpTotal());
        int posicao = calcularPosicaoRanking(pontuacao.getCliente().getId());

        return new MeuPerfilGameResponse(
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

    public List<RankingDetalhadoResponse> obterRankingTop5() {
        // Correção de Wildcard: Usando tipo explícito em vez de List<?>
        List<PontuacaoUsuario> top = pontuacaoRepository
                .findTopByOrderByXpTotalDesc(PageRequest.of(0, 5));

        List<RankingDetalhadoResponse> ranking = new ArrayList<>();

        for (int i = 0; i < top.size(); i++) {
            PontuacaoUsuario p = top.get(i);
            NivelUsuario nivel = p.getNivel();
            String nome = (p.getCliente() != null) ? p.getCliente().getNome() : "Desconhecido";

            ranking.add(new RankingDetalhadoResponse(
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

    /**
     * Ranking público LGPD-compliant.
     * Retorna posicao, primeiroNome, inicialSobrenome, nivel, badge, xpTotal, xpProximoNivel.
     * Nunca expõe e-mail, CPF ou sobrenome completo.
     *
     * @param limite  máximo de posições (capped em 100)
     * @param periodo "mes", "ano" ou "todos"
     */
    public List<RankingPublicResponse> obterRankingPublico(int limite, String periodo) {
        int cap = Math.min(Math.max(limite, 1), 100);

        LocalDateTime desde = null;
        if ("mes".equalsIgnoreCase(periodo)) {
            desde = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        } else if ("ano".equalsIgnoreCase(periodo)) {
            desde = LocalDate.now().withDayOfYear(1).atStartOfDay();
        }

        List<PontuacaoUsuario> top;
        if (desde == null) {
            top = pontuacaoRepository.findRankingTodos(PageRequest.of(0, cap));
        } else {
            top = pontuacaoRepository.findRankingDesde(desde, PageRequest.of(0, cap));
            if (top.size() < cap) {
                top = pontuacaoRepository.findRankingTodos(PageRequest.of(0, cap));
            }
        }

        List<RankingPublicResponse> result = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            PontuacaoUsuario p = top.get(i);
            String nomeCompleto = (p.getCliente() != null && p.getCliente().getNome() != null)
                ? p.getCliente().getNome().trim() : "Leitor";
            String[] partes = nomeCompleto.split("\\s+");
            String primeiroNome = partes[0];
            String inicialSobrenome = partes.length > 1
                ? partes[partes.length - 1].substring(0, 1).toUpperCase() + "." : "";

            NivelUsuario nivel = p.getNivel();
            int xpAtual = p.getXpTotal() != null ? p.getXpTotal() : 0;
            int xpProximo = calcularXpParaProximoNivel(nivel, xpAtual);
            String fotoPerfil = (p.getCliente() != null) ? p.getCliente().getFotoPerfil() : null;

            result.add(new RankingPublicResponse(
                i + 1, primeiroNome, inicialSobrenome,
                nivel.getDescricao(), nivel.getBadge(), xpAtual, xpProximo, fotoPerfil
            ));
        }
        return result;
    }

    // ==========================================
    // HELPERS
    // ==========================================

    private PontuacaoUsuario criarPontuacaoInicial(@NonNull Long clienteId) {
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
                return Math.max(0, niveis[i + 1].getXpMinimo() - xpAtual);
            }
        }
        return 0;
    }

    private int calcularPosicaoRanking(Long clienteId) {
        return pontuacaoRepository.findByClienteId(clienteId)
                .map(p -> (int) pontuacaoRepository.countByXpTotalGreaterThan(p.getXpTotal()) + 1)
                .orElse((int) pontuacaoRepository.count() + 1);
    }

    private MeuPerfilGameResponse criarPerfilVazio(String email) {
        return new MeuPerfilGameResponse(
                email, 0, NivelUsuario.INICIANTE.getDescricao(),
                NivelUsuario.INICIANTE.getBadge(), NivelUsuario.BRONZE.getXpMinimo(),
                (int) pontuacaoRepository.count() + 1, 0, 0, 0);
    }
}