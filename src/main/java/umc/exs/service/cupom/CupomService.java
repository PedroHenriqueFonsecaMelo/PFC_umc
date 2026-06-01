package umc.exs.service.cupom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.request.admin.CriarCupomRequest;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.CupomUso;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.CupomUsoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CupomService {

    private static final double DESCONTO_CUPOM_XP = 10.0;
    private static final int DIAS_VALIDADE_PADRAO = 30;

    private static final String LOG_CUPOM_XP_GERADO = "CUPOM_XP_GERADO";
    private static final String LOG_CUPOM_ADMIN_CRIADO = "CUPOM_ADMIN_CRIADO";
    private static final String LOG_CUPOM_INVALIDADO = "CUPOM_INVALIDADO";
    private static final String LOG_CUPOM_VALIDACAO = "CUPOM_VALIDADO";
    private static final String LOG_CUPOM_APLICADO = "CUPOM_APLICADO";
    private static final String LOG_CUPOM_LISTAGEM = "CUPOM_LISTAGEM";

    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado";
    private static final String CUPOM_NAO_ENCONTRADO = "Cupom não encontrado";

    private final CupomRepository cupomRepository;
    private final CupomUsoRepository cupomUsoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    // ───────────────────────── XP ─────────────────────────

    @Transactional
    public Cupom gerarCupomPorPontuacao(@NonNull Long clienteId) {

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        Cupom cupom = cupomRepository.save(Cupom.builder()
                .cliente(cliente)
                .codigo(gerarCodigoUnico("XP"))
                .percentualDesconto(DESCONTO_CUPOM_XP)
                .quantidadeMaxima(1)
                .tipo("PONTUACAO")
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE_PADRAO))
                .build());

        logAuditoria.registrarLog(
                LOG_CUPOM_XP_GERADO,
                cliente.getId(),
                cliente.getEmail(),
                "cupomId=" + cupom.getId() + ", codigo=" + cupom.getCodigo()
        );

        return cupom;
    }

    // ───────────────────────── ADMIN ─────────────────────────

    @Transactional
    public Cupom criarCupom(CriarCupomRequest dto, LocalDateTime dataValidade) {

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

        Cupom saved = cupomRepository.save(cupom);

        logAuditoria.registrarLog(
                LOG_CUPOM_ADMIN_CRIADO,
                null,
                null,
                "cupomId=" + saved.getId() + ", codigo=" + codigoFinal
        );

        return saved;
    }

    @Transactional
    public void invalidarCupom(@NonNull Long id) {

        Cupom cupom = cupomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(CUPOM_NAO_ENCONTRADO));

        cupom.setUsado(true);
        cupomRepository.save(cupom);

        logAuditoria.registrarLog(
                LOG_CUPOM_INVALIDADO,
                null,
                null,
                "cupomId=" + id
        );
    }

    // ───────────────────────── VALIDAÇÃO ─────────────────────────

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> validarCupomParaTotal(String codigo, String emailCliente, double total) {

        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        Cupom cupom = cupomRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException(CUPOM_NAO_ENCONTRADO));

        verificarElegibilidade(cupom, cliente);

        double desconto = total * (cupom.getPercentualDesconto() / 100.0);
        double totalComDesconto = Math.max(0, total - desconto);

        logAuditoria.registrarLog(
                LOG_CUPOM_VALIDACAO,
                cliente.getId(),
                cliente.getEmail(),
                "cupom=" + cupom.getCodigo() + ", total=" + total
        );

        return java.util.Map.of(
                "valido", true,
                "codigo", cupom.getCodigo(),
                "percentual", cupom.getPercentualDesconto(),
                "totalOriginal", total,
                "desconto", desconto,
                "totalComDesconto", totalComDesconto
        );
    }

    // ───────────────────────── APLICAÇÃO ─────────────────────────

    @Transactional
    public double aplicarCupomCarrinho(String codigo, Cliente cliente, double totalOriginal) {

        Cupom cupom = cupomRepository.findByCodigo(codigo.toUpperCase())
                .orElseThrow(() -> new RuntimeException(CUPOM_NAO_ENCONTRADO));

        verificarElegibilidade(cupom, cliente);

        CupomUso uso = CupomUso.builder()
                .cupom(cupom)
                .cliente(cliente)
                .livroId(null)
                .dataUso(LocalDateTime.now())
                .build();

        cupomUsoRepository.save(uso);

        cupom.setQuantidadeUsada(cupom.getQuantidadeUsada() + 1);

        if (cupom.getQuantidadeMaxima() != null &&
                cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            cupom.setUsado(true);
        }

        cupomRepository.save(cupom);

        double totalFinal = calcularPrecoComDesconto(totalOriginal, cupom.getPercentualDesconto());

        logAuditoria.registrarLog(
                LOG_CUPOM_APLICADO,
                cliente.getId(),
                cliente.getEmail(),
                "cupom=" + cupom.getCodigo() + ", totalOriginal=" + totalOriginal + ", totalFinal=" + totalFinal
        );

        return totalFinal;
    }

    // ───────────────────────── HELPERS ─────────────────────────

    private void verificarElegibilidade(Cupom cupom, Cliente cliente) {

        if (cupom.getExpiracao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cupom expirado");
        }

        if (cupom.isUsado()) {
            throw new IllegalArgumentException("Cupom inativo");
        }

        if (cupom.getCliente() != null &&
                !cupom.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Cupom exclusivo para outro usuário");
        }

        if (cupomUsoRepository.existsByCupomIdAndClienteId(cupom.getId(), cliente.getId())) {
            throw new IllegalArgumentException("Cupom já utilizado");
        }

        if (cupom.getQuantidadeMaxima() != null &&
                cupom.getQuantidadeUsada() >= cupom.getQuantidadeMaxima()) {
            throw new IllegalArgumentException("Limite de uso atingido");
        }
    }

    private double calcularPrecoComDesconto(double preco, double percentual) {
        double desconto = preco * (percentual / 100.0);
        return Math.max(0, preco - desconto);
    }

    private void validarPercentual(Double p) {
        if (p == null || p <= 0 || p > 100)
            throw new IllegalArgumentException("Percentual inválido");
    }

    private void validarData(LocalDateTime d) {
        if (d == null || d.isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("Data inválida");
    }

    private String gerarCodigoUnico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // ───────────────────────── LISTAGENS ─────────────────────────

    public List<Cupom> listarTodosCupons() {

        List<Cupom> cupons = cupomRepository.findAllByOrderByDataCriacaoDesc().stream().toList();

        logAuditoria.registrarLog(
                LOG_CUPOM_LISTAGEM,
                null,
                null,
                "total=" + cupons.size()
        );

        return cupons;
    }

    public List<Cupom> listarCuponsDisponiveis(String emailCliente) {

        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

        List<Cupom> cupons = cupomRepository
                .findByClienteIdAndUsadoFalseAndExpiracaoAfter(cliente.getId(), LocalDateTime.now())
                .stream().toList();

        logAuditoria.registrarLog(
                LOG_CUPOM_LISTAGEM,
                cliente.getId(),
                cliente.getEmail(),
                "disponiveis=" + cupons.size()
        );

        return cupons;
    }
}