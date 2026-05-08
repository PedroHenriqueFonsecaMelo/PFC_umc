package umc.exs.service.cupom;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CupomDTO;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.CupomUso;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.CupomUsoRepository;
import umc.exs.repository.usuario.ClienteRepository;

/**
 * Gerencia o ciclo de vida dos cupons de desconto:
 * - Geração automática por XP (cupom nominal, desconto fixo)
 * - Criação manual pelo admin (código, %, quantidade, validade)
 * - Validação sem uso (para preview do desconto na vitrine)
 * - Aplicação na compra (registra uso, impede duplicata)
 * - Listagem e invalidação
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CupomService {

    private static final double DESCONTO_CUPOM_XP = 10.0; // 10% off
    private static final int DIAS_VALIDADE_PADRAO = 30;

    private final CupomRepository cupomRepository;
    private final CupomUsoRepository cupomUsoRepository;
    private final ClienteRepository clienteRepository;
    private final LivroRepository livroRepository;

    // ── Geração automática (XP) ───────────────────────────────────────────────

    /**
     * Gera cupom de 10% de desconto quando o cliente atinge múltiplo de 500 XP.
     * Uso único e nominativo.
     */
    @Transactional
    public Cupom gerarCupomPorPontuacao(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));

        String codigo = gerarCodigoUnico("XP");
        Cupom cupom = Cupom.builder()
                .cliente(cliente)
                .codigo(codigo)
                .percentualDesconto(DESCONTO_CUPOM_XP)
                .quantidadeMaxima(1)
                .tipo("PONTUACAO")
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE_PADRAO))
                .build();

        cupomRepository.save(cupom);
        log.info("Cupom XP '{}' gerado para cliente ID {} ({}% off, uso único).",
                codigo, clienteId, DESCONTO_CUPOM_XP);
        return cupom;
    }

    // ── Criação pelo admin ────────────────────────────────────────────────────

    /**
     * Admin cria cupom com código customizado ou gerado automaticamente.
     *
     * @param codigo            código desejado — null/vazio = gerado com prefixo PROMO
     * @param percentualDesconto percentual de desconto (1–100)
     * @param dataValidade      data/hora de expiração
     * @param quantidadeMaxima  usos máximos — null = ilimitado
     * @param clienteId         destinatário — null = cupom público
     */
    @Transactional
    public Cupom criarCupom(String codigo, Double percentualDesconto,
                             LocalDateTime dataValidade, Integer quantidadeMaxima, Long clienteId) {
        if (percentualDesconto == null || percentualDesconto <= 0 || percentualDesconto > 100) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 1 e 100.");
        }
        if (dataValidade == null || dataValidade.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data de validade inválida ou já expirada.");
        }

        Cliente cliente = null;
        if (clienteId != null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));
        }

        String codigoFinal = (codigo != null && !codigo.isBlank())
                ? codigo.trim().toUpperCase()
                : gerarCodigoUnico("PROMO");

        if (cupomRepository.existsByCodigo(codigoFinal)) {
            throw new IllegalArgumentException("Código de cupom já em uso: " + codigoFinal);
        }

        Cupom cupom = Cupom.builder()
                .cliente(cliente)
                .codigo(codigoFinal)
                .percentualDesconto(percentualDesconto)
                .quantidadeMaxima(quantidadeMaxima)
                .tipo("PROMOCIONAL")
                .dataCriacao(LocalDateTime.now())
                .expiracao(dataValidade)
                .build();

        cupomRepository.save(cupom);
        log.info("Cupom '{}' criado: {}% off | maxUsos={} | cliente={}.",
                codigoFinal, percentualDesconto, quantidadeMaxima,
                clienteId != null ? clienteId : "público");
        return cupom;
    }

    // ── Validação (sem registrar uso) ─────────────────────────────────────────

    /**
     * Valida se o cupom pode ser aplicado a um livro por um cliente.
     * Não registra uso — serve apenas para preview do desconto.
     *
     * @return mapa com: valido, percentual, precoOriginal, precoComDesconto, economia, mensagem
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validarCupom(String codigo, String emailCliente, Long livroId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Cliente cliente = clienteRepository.findByEmail(emailCliente)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado."));

            Cupom cupom = cupomRepository.findByCodigo(codigo.trim().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado."));

            verificarElegibilidade(cupom, cliente);

            var livro = livroRepository.findByIdAndAprovadoTrue(livroId)
                    .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado."));

            double precoOriginal = livro.getPrecoAprovado() != null ? livro.getPrecoAprovado() : 0.0;
            double economia = precoOriginal * (cupom.getPercentualDesconto() / 100.0);
            double precoFinal = Math.max(0, precoOriginal - economia);

            result.put("valido", true);
            result.put("percentual", cupom.getPercentualDesconto());
            result.put("precoOriginal", precoOriginal);
            result.put("precoComDesconto", precoFinal);
            result.put("economia", economia);
            result.put("mensagem", String.format("Cupom aplicado! %.0f%% de desconto.",
                    cupom.getPercentualDesconto()));

        } catch (IllegalArgumentException e) {
            result.put("valido", false);
            result.put("mensagem", e.getMessage());
        }
        return result;
    }

    // ── Aplicação na compra ───────────────────────────────────────────────────

    /**
     * Aplica cupom a uma compra: valida elegibilidade, registra uso e retorna o preço final.
     * Chamado internamente durante o processamento do carrinho.
     *
     * @param codigo        código do cupom
     * @param cliente       comprador
     * @param livroId       ID do livro ao qual o desconto será aplicado
     * @param precoOriginal preço original do livro (precoAprovado)
     * @return preço final após desconto
     */
    @Transactional
    public double aplicarCupom(String codigo, Cliente cliente, Long livroId, double precoOriginal) {
        Cupom cupom = cupomRepository.findByCodigo(codigo.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado: " + codigo));

        verificarElegibilidade(cupom, cliente);

        // Registra uso
        CupomUso uso = CupomUso.builder()
                .cupom(cupom)
                .cliente(cliente)
                .livroId(livroId)
                .dataUso(LocalDateTime.now())
                .build();
        cupomUsoRepository.save(uso);

        // Incrementa contador e esgota se atingiu o limite
        cupom.setQuantidadeUsada(cupom.getQuantidadeUsada() + 1);
        if (cupom.getQuantidadeMaxima() != null
                && cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            cupom.setUsado(true);
        }
        cupomRepository.save(cupom);

        double desconto = precoOriginal * (cupom.getPercentualDesconto() / 100.0);
        double precoFinal = Math.max(0, precoOriginal - desconto);

        log.info("Cupom '{}' aplicado: livro={} | cliente={} | {:.0f}% off | T${} → T${}",
                codigo, livroId, cliente.getEmail(),
                cupom.getPercentualDesconto(), precoOriginal, precoFinal);
        return precoFinal;
    }

    // ── Listagem ──────────────────────────────────────────────────────────────

    /** Cupons disponíveis do cliente (não usados, não expirados). */
    public List<CupomDTO> listarCuponsDisponiveis(String emailCliente) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return cupomRepository
                .findByClienteIdAndUsadoFalseAndExpiracaoAfter(cliente.getId(), LocalDateTime.now())
                .stream().map(this::toDTO).toList();
    }

    /** Todos os cupons do sistema, do mais recente para o mais antigo (visão admin). */
    public List<CupomDTO> listarTodosCupons() {
        return cupomRepository.findAllByOrderByDataCriacaoDesc()
                .stream().map(this::toDTOAdmin).toList();
    }

    // ── Invalidação manual ────────────────────────────────────────────────────

    /** Admin invalida um cupom sem registrar uso. */
    @Transactional
    public void invalidarCupom(Long id) {
        Cupom cupom = cupomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado: " + id));
        if (cupom.isUsado()) throw new IllegalArgumentException("Cupom já está invalidado.");
        cupom.setUsado(true);
        cupomRepository.save(cupom);
        log.info("Cupom '{}' (ID {}) invalidado manualmente pelo admin.", cupom.getCodigo(), id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void verificarElegibilidade(Cupom cupom, Cliente cliente) {
        if (cupom.isUsado()) {
            throw new IllegalArgumentException("Cupom inativo.");
        }
        if (LocalDateTime.now().isAfter(cupom.getExpiracao())) {
            throw new IllegalArgumentException("Cupom expirado.");
        }
        if (cupom.getCliente() != null && !cupom.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Este cupom não pertence ao seu perfil.");
        }
        if (cupomUsoRepository.existsByCupomIdAndClienteId(cupom.getId(), cliente.getId())) {
            throw new IllegalArgumentException("Você já utilizou este cupom.");
        }
        if (cupom.getQuantidadeMaxima() != null
                && cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            throw new IllegalArgumentException("Cupom esgotado.");
        }
    }

    private String gerarCodigoUnico(String prefixo) {
        String codigo;
        int tentativas = 0;
        do {
            codigo = prefixo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            tentativas++;
        } while (cupomRepository.existsByCodigo(codigo) && tentativas < 10);
        return codigo;
    }

    public CupomDTO toDTO(Cupom c) {
        return CupomDTO.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .percentualDesconto(c.getPercentualDesconto())
                .expiracao(c.getExpiracao())
                .usado(c.isUsado())
                .tipo(c.getTipo())
                .dataCriacao(c.getDataCriacao())
                .quantidadeMaxima(c.getQuantidadeMaxima())
                .quantidadeUsada(c.getQuantidadeUsada())
                .build();
    }

    private CupomDTO toDTOAdmin(Cupom c) {
        CupomDTO dto = toDTO(c);
        if (c.getCliente() != null) {
            dto.setClienteNome(c.getCliente().getNome());
            dto.setClienteEmail(c.getCliente().getEmail());
        }
        return dto;
    }
}
