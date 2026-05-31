package umc.exs.service.core.livros.delegado;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.extern.ItemResultadoDTO;
import umc.exs.dto.request.compra.CarrinhoCompraRequest;
import umc.exs.dto.response.compras.CarrinhoCompraResponse;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroCompraService {

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;

    private final EmailFacade emailFacade;
    private final PedidoService pedidoService;
    private final CupomService cupomService;
    private final GamificacaoService gamificacaoService;
    private final LogAuditoriaService logAuditoria;
    private final NotificacaoService notificacaoService;

    private static final String ASSUNTO_SALDO = "Atualização de saldo — Bibliotroca";

    @Transactional
    public void realizarCompra(@NonNull Long livroId, String emailComprador) {
        Livro livro = livroRepository.findByIdAndAprovadoTrueWithLock(livroId)
                .orElseThrow(() -> new IllegalArgumentException("Livro indisponível ou não encontrado."));

        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new IllegalStateException("Comprador não encontrado."));

        if (comprador.getEnderecos() == null || comprador.getEnderecos().isEmpty()) {
            throw new IllegalStateException("É necessário cadastrar um endereço de entrega antes de comprar.");
        }

        if (comprador.getSaldoTokens() < livro.getPrecoAprovado()) {
            throw new IllegalStateException("Saldo insuficiente para completar a compra.");
        }

        // Captura vendedor antes de deletar o livro
        Cliente vendedor = livro.getVendedor();
        if (vendedor == null && livro.getLote() != null) {
            vendedor = livro.getLote().getCliente();
        }
        final Cliente vendedorFinal = vendedor;
        final String tituloLivro = livro.getTitulo();
        final double precoLivro = livro.getPrecoAprovado();

        double saldoAntes = comprador.getSaldoTokens();
        processarBaixaLivro(comprador, livro);

        enviarEmailsCompraUnica(comprador, livro, saldoAntes);

        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + precoLivro);

        gamificacaoService.xpCompra(comprador.getId());

        // Notificação dashboard: compra realizada (comprador)
        try {
            notificacaoService.criarNotificacaoDashboard(
                    comprador,
                    String.format("Compra realizada! T$ %.2f debitados. Saldo atual: T$ %.2f.",
                            precoLivro, comprador.getSaldoTokens()),
                    "/clientes/homepage?aba=pedidos");
        } catch (Exception e) {
            log.error("Erro ao notificar comprador {}: {}", comprador.getEmail(), e.getMessage());
        }

        // Notificação dashboard: livro vendido (vendedor)
        if (vendedorFinal != null) {
            try {
                notificacaoService.criarNotificacaoDashboard(
                        vendedorFinal,
                        String.format("Seu livro '%s' foi vendido!", tituloLivro),
                        "/clientes/homepage?aba=pedidos");
            } catch (Exception e) {
                log.error("Erro ao notificar vendedor {}: {}", vendedorFinal.getEmail(), e.getMessage());
            }
        }

        emailFacade.sendHtmlSafe(
                comprador.getEmail(),
                "Compra realizada",
                "Seu livro foi comprado com sucesso");
    }

    @Transactional
    public CarrinhoCompraResponse comprarCarrinho(String emailComprador, CarrinhoCompraRequest request) {
        Cliente comprador = validarCompradorCarrinho(emailComprador);

        List<Long> ids = Objects.requireNonNull(request.getLivroIds(), "Lista de IDs não pode ser nula");
        if (ids.isEmpty())
            throw new IllegalArgumentException("O carrinho está vazio.");

        List<Livro> livrosParaComprar = livroRepository.findAllDisponiveisWithLock(ids).stream()
                .filter(l -> Boolean.TRUE.equals(l.getAprovado()))
                .toList();

        if (livrosParaComprar.isEmpty()) {
            throw new IllegalArgumentException("Livro indisponível ou não encontrado.");
        }

        List<ItemResultadoDTO> falhas = identificarFalhas(ids, livrosParaComprar);

        // Calcula o total original dos livros disponíveis
        double totalOriginal = livrosParaComprar.stream()
                .mapToDouble(l -> l.getPrecoAprovado() != null ? l.getPrecoAprovado() : 0.0)
                .sum();

        // Aplica cupom no total, se informado
        String codigoCupom = request.getCodigoCupom();
        double totalComDesconto = totalOriginal;
        double descontoAplicado = 0.0;
        String cupomAplicado = null;

        if (codigoCupom != null && !codigoCupom.isBlank()) {
            totalComDesconto = cupomService.aplicarCupomCarrinho(codigoCupom, comprador, totalOriginal);
            descontoAplicado = totalOriginal - totalComDesconto;
            cupomAplicado = codigoCupom.toUpperCase().trim();
        }

        // Valida saldo contra o total com desconto
        if (comprador.getSaldoTokens() < totalComDesconto) {
            throw new IllegalStateException(
                    String.format("Saldo insuficiente. Necessário: T$ %.2f", totalComDesconto));
        }

        double saldoAnterior = comprador.getSaldoTokens();

        // Processa cada livro (registra pedido + remove), SEM debitar por item
        List<ItemResultadoDTO> comprados = registrarLivrosCarrinho(comprador, livrosParaComprar, falhas);

        // Se nenhum item foi processado com sucesso, não debitar tokens
        if (comprados.isEmpty()) {
            String motivo = falhas.isEmpty()
                    ? "Não foi possível processar a compra."
                    : falhas.get(0).getMotivo().replaceFirst("^Erro: ", "");
            throw new IllegalStateException(motivo);
        }

        // Debita o total (com desconto) de uma vez
        comprador.setSaldoTokens(saldoAnterior - totalComDesconto);
        clienteRepository.save(comprador);

        registrarLogECoordenarEmails(comprador, comprados, falhas, saldoAnterior);

        // Notificação dashboard: compra via carrinho (comprador)
        if (!comprados.isEmpty()) {
            try {
                notificacaoService.criarNotificacaoDashboard(
                        comprador,
                        String.format("Compra realizada! T$ %.2f debitados. Saldo atual: T$ %.2f.",
                                saldoAnterior - comprador.getSaldoTokens(), comprador.getSaldoTokens()),
                        "/clientes/homepage?aba=pedidos");
            } catch (Exception e) {
                log.error("Erro ao notificar comprador carrinho {}: {}", comprador.getEmail(), e.getMessage());
            }
        }

        return CarrinhoCompraResponse.builder()
                .totalSolicitados(ids.size())
                .totalComprados(comprados.size())
                .totalOriginal(totalOriginal)
                .descontoAplicado(descontoAplicado)
                .codigoCupomAplicado(cupomAplicado)
                .totalGasto(saldoAnterior - comprador.getSaldoTokens())
                .saldoRestante(comprador.getSaldoTokens())
                .comprados(comprados)
                .falhas(falhas)
                .build();
    }

    private Cliente validarCompradorCarrinho(String email) {
        Cliente c = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Comprador não encontrado."));
        if (c.isBloqueada()) {
            throw new IllegalStateException("Sua conta está bloqueada.");
        }
        if (c.getEnderecos() == null || c.getEnderecos().isEmpty()) {
            throw new IllegalStateException("É necessário cadastrar um endereço de entrega antes de comprar.");
        }
        return c;
    }

    private List<ItemResultadoDTO> identificarFalhas(List<Long> idsSolicitados, List<Livro> encontrados) {
        Set<Long> encontradosIds = encontrados.stream().map(Livro::getId).collect(Collectors.toSet());
        return idsSolicitados.stream()
                .filter(id -> !encontradosIds.contains(id))
                .map(id -> ItemResultadoDTO.builder().livroId(id).motivo("Indisponível.").build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Registra pedido e remove cada livro do estoque.
     * O débito do saldo ocorre em bloco no comprarCarrinho (com desconto aplicado).
     */
    private List<ItemResultadoDTO> registrarLivrosCarrinho(Cliente comprador, List<Livro> livros,
            List<ItemResultadoDTO> falhas) {
        List<ItemResultadoDTO> sucesso = new ArrayList<>();
        // Um único código agrupa todos os itens desta compra de carrinho
        String codigoCompra = pedidoService.gerarCodigoPedido();
        for (Livro livro : livros) {
            try {
                // Captura vendedor e título antes de deletar o livro
                final String titulo = livro.getTitulo();
                Cliente vendedor = livro.getVendedor();
                if (vendedor == null && livro.getLote() != null) {
                    vendedor = livro.getLote().getCliente();
                }
                final Cliente vendedorFinal = vendedor;

                livro.setAprovado(false);
                Pedido pedidoRegistrado = pedidoService.registrarPedido(comprador, livro, codigoCompra);

                livroRepository.save(livro);
                sucesso.add(ItemResultadoDTO.builder()
                        .livroId(livro.getId())
                        .pedidoId(pedidoRegistrado.getId())
                        .codigoPedido(pedidoRegistrado.getCodigoPedido())
                        .titulo(titulo)
                        .preco(livro.getPrecoAprovado())
                        .build());
                gamificacaoService.xpCompra(comprador.getId());

                // Notificação dashboard: livro vendido (vendedor)
                if (vendedorFinal != null) {
                    try {
                        notificacaoService.criarNotificacaoDashboard(
                                vendedorFinal,
                                String.format("Seu livro '%s' foi vendido!", titulo),
                                "/clientes/homepage?aba=pedidos");
                    } catch (Exception ne) {
                        log.error("Erro ao notificar vendedor {}: {}", vendedorFinal.getEmail(), ne.getMessage());
                    }
                }
            } catch (Exception e) {
                falhas.add(ItemResultadoDTO.builder().livroId(livro.getId()).motivo("Erro: " + e.getMessage()).build());
            }
        }
        return sucesso;
    }

    private void processarBaixaLivro(Cliente comprador, Livro livro) {
        pedidoService.registrarPedido(comprador, livro);
        comprador.setSaldoTokens(comprador.getSaldoTokens() - livro.getPrecoAprovado());
        livro.setAprovado(false);
        livroRepository.save(livro);
    }

    private void registrarLogECoordenarEmails(Cliente comprador, List<ItemResultadoDTO> comprados,
            List<ItemResultadoDTO> falhas, double saldoAnterior) {
        double totalGasto = saldoAnterior - comprador.getSaldoTokens();
        logAuditoria.registrarLog("COMPRA_CARRINHO", comprador.getId(), comprador.getEmail(),
                String.format("%d comprados, %d falhas.", comprados.size(), falhas.size()));

        if (!comprados.isEmpty()) {
            enviarEmailsSucessoCarrinho(comprador, comprados, totalGasto, saldoAnterior);
        }
    }

    private void enviarEmailsCompraUnica(Cliente comprador, Livro livro, double saldoAntes) {
        try {
            String email = Objects.requireNonNull(comprador.getEmail());
            String nome = Objects.requireNonNull(comprador.getNome());

            emailFacade.sendHtmlSafe(email, "Compra realizada com sucesso! — Bibliotroca",
                    EmailHtmlBuilder.compraSucesso(nome, livro.getTitulo(), livro.getPrecoAprovado(),
                            comprador.getSaldoTokens(), baseUrl));

            emailFacade.sendHtmlSafe(email, ASSUNTO_SALDO,
                    EmailHtmlBuilder.atualizacaoSaldo(nome, saldoAntes, livro.getPrecoAprovado(),
                            comprador.getSaldoTokens(), "Compra: " + livro.getTitulo(), false, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail: {}", e.getMessage());
        }
    }

    private void enviarEmailsSucessoCarrinho(Cliente comprador, List<ItemResultadoDTO> comprados, double totalGasto,
            double saldoAnterior) {
        try {
            String email = Objects.requireNonNull(comprador.getEmail());
            String nome = Objects.requireNonNull(comprador.getNome());

            List<String[]> itens = comprados.stream()
                    .map(i -> new String[] { i.getTitulo(), String.format("%.2f", i.getPreco()) }).toList();

            emailFacade.sendHtmlSafe(email, "Compra do carrinho confirmada! — Bibliotroca",
                    EmailHtmlBuilder.carrinhoConfirmado(nome, itens, totalGasto, comprador.getSaldoTokens(), baseUrl));

            emailFacade.sendHtmlSafe(email, ASSUNTO_SALDO,
                    EmailHtmlBuilder.atualizacaoSaldo(nome, saldoAnterior, totalGasto,
                            comprador.getSaldoTokens(), "Compra via carrinho", false, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail carrinho: {}", e.getMessage());
        }
    }

}