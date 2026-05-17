package umc.exs.service.cupom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.CupomUso;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.CupomUsoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.dto.compra.CriarCupomDTO;
import umc.exs.dto.compra.cupom.CupomDTO;
import umc.exs.mappers.CupomMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class CupomService {

    private static final double DESCONTO_CUPOM_XP = 10.0;
    private static final int DIAS_VALIDADE_PADRAO = 30;

    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado";
    private static final String CUPOM_NAO_ENCONTRADO = "Cupom não encontrado";

    private final CupomRepository cupomRepository;
    private final CupomUsoRepository cupomUsoRepository;
    private final ClienteRepository clienteRepository;
    private final CupomMapper cupomMapper;

    // ───────────────────────── XP (Cupons Gerados por Gamificação)
    // ─────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public Cupom gerarCupomPorPontuacao(@NonNull Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        return cupomRepository.save(Cupom.builder()
                .cliente(cliente)
                .codigo(gerarCodigoUnico("XP"))
                .percentualDesconto(DESCONTO_CUPOM_XP)
                .quantidadeMaxima(1) // Cupom de XP é sempre único
                .tipo("PONTUACAO")
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE_PADRAO))
                .build());
    }

    // ───────────────────────── ADMIN (Cupons Promocionais Estilo iFood)
    // ─────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public Cupom criarCupom(CriarCupomDTO dto, LocalDateTime dataValidade) {
        // Validações básicas
        validarPercentual(dto.getPercentualDesconto());
        validarData(dataValidade);

        String codigoFinal = (dto.getCodigo() == null || dto.getCodigo().isBlank())
                ? gerarCodigoUnico("PROMO")
                : dto.getCodigo().trim().toUpperCase();

        if (cupomRepository.existsByCodigo(codigoFinal)) {
            throw new IllegalArgumentException("Código de cupom já existente: " + codigoFinal);
        }

        Cliente clienteAlvo = null;
        if (dto.getClienteId() != null) {
            clienteAlvo = clienteRepository.findById(dto.getClienteId())
                    .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));
        }

        Cupom cupom = Cupom.builder()
                .codigo(codigoFinal)
                .cliente(clienteAlvo)
                .percentualDesconto(dto.getPercentualDesconto())
                .quantidadeMaxima(dto.getQuantidadeMaxima())
                .expiracao(dataValidade)
                .tipo("PROMOCIONAL")
                .dataCriacao(LocalDateTime.now())
                .usado(false)
                .quantidadeUsada(0)
                .build();

        return cupomRepository.save(cupom);
    }

    @Transactional
    public void invalidarCupom(@NonNull Long id) {
        Cupom cupom = cupomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CUPOM_NAO_ENCONTRADO));

        cupom.setUsado(true);
        cupomRepository.save(cupom);

        log.info("📢 Cupom ID {} foi invalidado manualmente.", id);
    }

    // ───────────────────────── VALIDAÇÃO E APLICAÇÃO ─────────────────────────

    /**
     * Valida o cupom sobre o total do carrinho e retorna preview do desconto.
     * Não registra uso.
     */
    @Transactional(readOnly = true)
    public java.util.Map<String, Object> validarCupomParaTotal(String codigo, String emailCliente, double total) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        Cupom cupom = cupomRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado."));

        verificarElegibilidade(cupom, cliente);

        double desconto = total * (cupom.getPercentualDesconto() / 100.0);
        double totalComDesconto = Math.max(0, total - desconto);

        return java.util.Map.of(
                "valido", true,
                "codigo", cupom.getCodigo(),
                "percentual", cupom.getPercentualDesconto(),
                "totalOriginal", total,
                "desconto", desconto,
                "totalComDesconto", totalComDesconto
        );
    }

    /**
     * Aplica o cupom ao total do carrinho, registrando o uso único.
     * Retorna o total com desconto aplicado.
     */
    @SuppressWarnings("null")
    @Transactional
    public double aplicarCupomCarrinho(String codigo, Cliente cliente, double totalOriginal) {
        Cupom cupom = cupomRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException(CUPOM_NAO_ENCONTRADO));

        // Re-valida no momento da compra para evitar race conditions
        verificarElegibilidade(cupom, cliente);

        // Registra uso do cupom (livroId null = aplicado sobre o total do carrinho)
        CupomUso uso = CupomUso.builder()
                .cupom(cupom)
                .cliente(cliente)
                .livroId(null)
                .dataUso(LocalDateTime.now())
                .build();
        cupomUsoRepository.save(uso);

        // Atualiza contadores globais
        cupom.setQuantidadeUsada(cupom.getQuantidadeUsada() + 1);

        if (cupom.getQuantidadeMaxima() != null &&
                cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            cupom.setUsado(true);
        }

        cupomRepository.save(cupom);
        return calcularPrecoComDesconto(totalOriginal, cupom.getPercentualDesconto());
    }

    // ───────────────────────── HELPERS ─────────────────────────

    /**
     * Centraliza todas as regras de bloqueio (iFood Style)
     */
    private void verificarElegibilidade(Cupom cupom, Cliente cliente) {
        // 1. Verificar se está expirado
        if (cupom.getExpiracao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Este cupom expirou.");
        }

        // 2. Verificar se foi desativado manualmente ou esgotou
        if (cupom.isUsado()) {
            throw new IllegalArgumentException("Cupom não está mais ativo.");
        }

        // 3. Verificar se o cupom é exclusivo para outro cliente
        if (cupom.getCliente() != null && !cupom.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Este cupom é exclusivo para outro usuário.");
        }

        // 4. REGRA DE OURO: Uso único por CPF/Cliente
        if (cupomUsoRepository.existsByCupomIdAndClienteId(cupom.getId(), cliente.getId())) {
            throw new IllegalArgumentException("Você já utilizou este cupom.");
        }

        // 5. Verificar limite global
        if (cupom.getQuantidadeMaxima() != null && cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            throw new IllegalArgumentException("Este cupom atingiu o limite máximo de resgates.");
        }
    }

    private double calcularPrecoComDesconto(double preco, double percentual) {
        double desconto = preco * (percentual / 100.0);
        return Math.max(0, preco - desconto);
    }

    private void validarPercentual(Double p) {
        if (p == null || p <= 0 || p > 100)
            throw new IllegalArgumentException("Percentual de desconto inválido (1-100)");
    }

    private void validarData(LocalDateTime d) {
        if (d == null || d.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("A data de expiração deve ser no futuro");
    }

    private String gerarCodigoUnico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Listagens
    public List<CupomDTO> listarTodosCupons() {
        return cupomRepository.findAllByOrderByDataCriacaoDesc().stream().map(cupomMapper::toDTO).toList();
    }

    public List<CupomDTO> listarCuponsDisponiveis(String emailCliente) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));
        return cupomRepository.findByClienteIdAndUsadoFalseAndExpiracaoAfter(cliente.getId(), LocalDateTime.now())
                .stream().map(cupomMapper::toDTO).toList();
    }
}