package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.compra.ItemResultadoDTO;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraRequestDTO;
import umc.exs.dtos.compra.carrinho.CarrinhoCompraResponseDTO;
import umc.exs.dtos.livro.GoogleBookResponse;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.mappers.LivroMapper;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroCompraService {

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;

    private final EmailService emailService;
    private final PedidoService pedidoService;
    private final GamificacaoService gamificacaoService;
    private final LogAuditoriaService logAuditoria;

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

        double saldoAntes = comprador.getSaldoTokens();
        processarBaixaLivro(comprador, livro);

        enviarEmailsCompraUnica(comprador, livro, saldoAntes);

        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + livro.getPrecoAprovado());

        gamificacaoService.xpCompra(comprador.getId());

        emailService.enviar(
                comprador.getEmail(),
                "Compra realizada",
                "Seu livro foi comprado com sucesso");
    }

    @Transactional
    public CarrinhoCompraResponseDTO comprarCarrinho(String emailComprador, CarrinhoCompraRequestDTO request) {
        Cliente comprador = validarCompradorCarrinho(emailComprador);

        List<Long> ids = Objects.requireNonNull(request.getLivroIds(), "Lista de IDs não pode ser nula");
        if (ids.isEmpty())
            throw new IllegalArgumentException("O carrinho está vazio.");

        List<Livro> livrosParaComprar = livroRepository.findAllById(ids).stream()
                .filter(l -> Boolean.TRUE.equals(l.getAprovado()))
                .toList();

        List<ItemResultadoDTO> falhas = identificarFalhas(ids, livrosParaComprar);
        validarSaldoTotal(comprador, livrosParaComprar);

        double saldoAnterior = comprador.getSaldoTokens();
        List<ItemResultadoDTO> comprados = processarItensCarrinho(comprador, livrosParaComprar, falhas);

        clienteRepository.save(comprador);
        registrarLogECoordenarEmails(comprador, comprados, falhas, saldoAnterior);

        return CarrinhoCompraResponseDTO.builder()
                .totalSolicitados(ids.size())
                .totalComprados(comprados.size())
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

    private void validarSaldoTotal(Cliente comprador, List<Livro> livros) {
        double total = livros.stream().mapToDouble(l -> l.getPrecoAprovado() != null ? l.getPrecoAprovado() : 0.0)
                .sum();
        if (comprador.getSaldoTokens() < total) {
            throw new IllegalStateException(String.format("Saldo insuficiente. Necessário: T$ %.2f", total));
        }
    }

    private List<ItemResultadoDTO> processarItensCarrinho(Cliente comprador, List<Livro> livros,
            List<ItemResultadoDTO> falhas) {
        List<ItemResultadoDTO> sucesso = new ArrayList<>();
        for (Livro livro : livros) {
            try {
                processarBaixaLivro(comprador, livro);
                sucesso.add(ItemResultadoDTO.builder()
                        .livroId(livro.getId()).titulo(livro.getTitulo()).preco(livro.getPrecoAprovado()).build());
                gamificacaoService.xpCompra(comprador.getId());
            } catch (Exception e) {
                falhas.add(ItemResultadoDTO.builder().livroId(livro.getId()).motivo("Erro: " + e.getMessage()).build());
            }
        }
        return sucesso;
    }

    private void processarBaixaLivro(Cliente comprador, Livro livro) {
        pedidoService.registrarPedido(comprador, livro);
        comprador.setSaldoTokens(comprador.getSaldoTokens() - livro.getPrecoAprovado());
        livroRepository.delete(livro);
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

    @SuppressWarnings("null")
    private void enviarEmailsCompraUnica(Cliente comprador, Livro livro, double saldoAntes) {
        try {
            String email = Objects.requireNonNull(comprador.getEmail());
            String nome = Objects.requireNonNull(comprador.getNome());

            emailService.enviarHtml(email, "Compra realizada com sucesso! — Bibliotroca",
                    EmailHtmlBuilder.compraSucesso(nome, livro.getTitulo(), livro.getPrecoAprovado(),
                            comprador.getSaldoTokens(), baseUrl));

            emailService.enviarHtml(email, ASSUNTO_SALDO,
                    EmailHtmlBuilder.atualizacaoSaldo(nome, saldoAntes, livro.getPrecoAprovado(),
                            comprador.getSaldoTokens(), "Compra: " + livro.getTitulo(), false, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail: {}", e.getMessage());
        }
    }

    @SuppressWarnings("null")
    private void enviarEmailsSucessoCarrinho(Cliente comprador, List<ItemResultadoDTO> comprados, double totalGasto,
            double saldoAnterior) {
        try {
            String email = Objects.requireNonNull(comprador.getEmail());
            String nome = Objects.requireNonNull(comprador.getNome());

            List<String[]> itens = comprados.stream()
                    .map(i -> new String[] { i.getTitulo(), String.format("%.2f", i.getPreco()) }).toList();

            emailService.enviarHtml(email, "Compra do carrinho confirmada! — Bibliotroca",
                    EmailHtmlBuilder.carrinhoConfirmado(nome, itens, totalGasto, comprador.getSaldoTokens(), baseUrl));

            emailService.enviarHtml(email, ASSUNTO_SALDO,
                    EmailHtmlBuilder.atualizacaoSaldo(nome, saldoAnterior, totalGasto,
                            comprador.getSaldoTokens(), "Compra via carrinho", false, LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Erro ao enviar e-mail carrinho: {}", e.getMessage());
        }
    }

}