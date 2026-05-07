package umc.exs.service.cupom;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CupomDTO;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.carteira.CarteiraService;

/**
 * Gerencia o ciclo de vida dos cupons:
 * - Geração automática por XP (múltiplos de 500, valor = 10 tokens)
 * - Criação manual pelo admin (PROMOCIONAL)
 * - Resgate pelo cliente (crédito de tokens na carteira)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CupomService {

    private static final double TOKENS_POR_CUPOM_XP = 10.0;
    private static final int DIAS_VALIDADE = 30;

    private final CupomRepository cupomRepository;
    private final ClienteRepository clienteRepository;
    private final CarteiraService carteiraService;

    /**
     * Gera cupom de pontuação quando o cliente atinge múltiplo de 500 XP.
     * Chamado por {@code GamificacaoService.adicionarXp()} quando threshold atingido.
     */
    @Transactional
    public Cupom gerarCupomPorPontuacao(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));

        String codigo = gerarCodigoUnico("XP");
        Cupom cupom = Cupom.builder()
                .cliente(cliente)
                .codigo(codigo)
                .valorTokens(TOKENS_POR_CUPOM_XP)
                .tipo("PONTUACAO")
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE))
                .build();

        cupomRepository.save(cupom);
        log.info("Cupom de pontuação '{}' gerado para cliente ID {} (T$ {}).",
                codigo, clienteId, TOKENS_POR_CUPOM_XP);
        return cupom;
    }

    /**
     * Admin cria cupom promocional (público ou vinculado a cliente específico).
     *
     * @param valorTokens  valor em tokens do cupom
     * @param clienteId    ID do cliente destinatário, ou null para cupom público
     */
    @Transactional
    public Cupom criarCupomPromocional(Double valorTokens, Long clienteId) {
        Cliente cliente = null;
        if (clienteId != null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado: " + clienteId));
        }

        String codigo = gerarCodigoUnico("PROMO");
        Cupom cupom = Cupom.builder()
                .cliente(cliente)
                .codigo(codigo)
                .valorTokens(valorTokens)
                .tipo("PROMOCIONAL")
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(DIAS_VALIDADE))
                .build();

        cupomRepository.save(cupom);
        log.info("Cupom promocional '{}' criado pelo admin. Valor: T$ {}. Cliente: {}.",
                codigo, valorTokens, clienteId != null ? clienteId : "público");
        return cupom;
    }

    /**
     * Resgata cupom: valida, credita tokens e marca como usado.
     */
    @Transactional
    public CupomDTO resgatarCupom(String emailCliente, String codigo) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Cupom cupom = cupomRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Cupom não encontrado: " + codigo));

        if (cupom.isUsado()) {
            throw new IllegalArgumentException("Cupom já utilizado.");
        }
        if (LocalDateTime.now().isAfter(cupom.getExpiracao())) {
            throw new IllegalArgumentException("Cupom expirado.");
        }
        // Cupons de pontuação são nominais; promocionais podem ser públicos
        if (cupom.getCliente() != null && !cupom.getCliente().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("Este cupom não pertence ao seu perfil.");
        }

        // Credita tokens
        carteiraService.adicionarTokens(cliente, cupom.getValorTokens(), "CUPOM", cupom.getCodigo());

        cupom.setUsado(true);
        cupomRepository.save(cupom);

        log.info("Cupom '{}' resgatado por {} — T$ {}.", codigo, emailCliente, cupom.getValorTokens());
        return toDTO(cupom);
    }

    /** Lista cupons disponíveis (não usados e não expirados) do cliente. */
    public List<CupomDTO> listarCuponsDisponiveis(String emailCliente) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
        return cupomRepository
                .findByClienteIdAndUsadoFalseAndExpiracaoAfter(cliente.getId(), LocalDateTime.now())
                .stream().map(this::toDTO).toList();
    }

    /** Lista todos os cupons do sistema, do mais recente ao mais antigo (visão admin). */
    public List<CupomDTO> listarTodosCupons() {
        return cupomRepository.findAllByOrderByDataCriacaoDesc()
                .stream().map(this::toDTOAdmin).toList();
    }

    /** Invalida manualmente um cupom, marcando-o como usado sem creditar tokens. */
    @Transactional
    public void invalidarCupom(Long id) {
        Cupom cupom = cupomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado: " + id));
        if (cupom.isUsado()) {
            throw new IllegalArgumentException("Cupom já está invalidado.");
        }
        cupom.setUsado(true);
        cupomRepository.save(cupom);
        log.info("Cupom '{}' (ID {}) invalidado manualmente pelo admin.", cupom.getCodigo(), id);
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
                .valorTokens(c.getValorTokens())
                .expiracao(c.getExpiracao())
                .usado(c.isUsado())
                .tipo(c.getTipo())
                .dataCriacao(c.getDataCriacao())
                .build();
    }

    /** Versão admin do DTO: inclui nome e e-mail do cliente quando disponível. */
    private CupomDTO toDTOAdmin(Cupom c) {
        CupomDTO dto = toDTO(c);
        if (c.getCliente() != null) {
            dto.setClienteNome(c.getCliente().getNome());
            dto.setClienteEmail(c.getCliente().getEmail());
        }
        return dto;
    }
}
