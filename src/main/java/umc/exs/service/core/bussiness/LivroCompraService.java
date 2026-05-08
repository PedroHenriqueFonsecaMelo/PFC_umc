package umc.exs.service.core.bussiness;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.CarrinhoCompraRequestDTO;
import umc.exs.DTOs.compra.CarrinhoCompraResponseDTO;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.PedidoService;
import org.springframework.beans.factory.annotation.Value;
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

    @Transactional
    public void realizarCompra(@NonNull Long livroId, String emailComprador) {
        
        Livro livro = livroRepository.findByIdAndAprovadoTrueWithLock(livroId)
                .orElseThrow(() -> new RuntimeException("Livro indisponível"));

        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado"));

        if (comprador.getSaldoTokens() < livro.getPrecoAprovado()) {
            throw new RuntimeException("Saldo insuficiente");
        }

        comprador.setSaldoTokens(comprador.getSaldoTokens() - livro.getPrecoAprovado());

        // Registra pedido ANTES de deletar o livro
        pedidoService.registrarPedido(comprador, livro);

        // O livro sai do sistema
        livroRepository.delete(livro);
        clienteRepository.save(comprador);

        try {
            emailService.enviarHtml(
                    comprador.getEmail(),
                    "Compra realizada com sucesso! — Bibliotroca",
                    EmailHtmlBuilder.compraSucesso(comprador.getNome(), livro.getTitulo(),
                            livro.getPrecoAprovado(), comprador.getSaldoTokens(), baseUrl));
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de compra para {}: {}", comprador.getEmail(), e.getMessage());
        }
        
        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + livro.getPrecoAprovado());
        
        gamificacaoService.xpCompra(comprador.getId());
    }

    @Transactional
    public CarrinhoCompraResponseDTO comprarCarrinho(String emailComprador, CarrinhoCompraRequestDTO request) {

        // 1. Valida comprador
        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));

        if (comprador.isBloqueada()) {
            throw new RuntimeException("Sua conta está bloqueada. Entre em contato com o suporte.");
        }

        List<Long> ids = request.getLivroIds();
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("O carrinho está vazio.");
        }

        // 2. Busca todos os livros aprovados de uma vez
        List<Livro> livrosEncontrados = livroRepository.findAllById(ids)
                .stream()
                .filter(l -> Boolean.TRUE.equals(l.getAprovado()))
                .toList();

        // Detecta IDs que não foram encontrados ou não estão aprovados
        java.util.Set<Long> idsEncontrados = new java.util.HashSet<>();
        livrosEncontrados.forEach(l -> idsEncontrados.add(l.getId()));

        List<CarrinhoCompraResponseDTO.ItemResultado> falhas = new ArrayList<>();
        for (Long id : ids) {
            if (!idsEncontrados.contains(id)) {
                falhas.add(CarrinhoCompraResponseDTO.ItemResultado.builder()
                        .livroId(id)
                        .motivo("Livro não encontrado ou indisponível.")
                        .build());
            }
        }

        // 3. Verifica saldo total antes de debitar qualquer valor
        double totalNecessario = livrosEncontrados.stream()
                .mapToDouble(l -> l.getPrecoAprovado() != null ? l.getPrecoAprovado() : 0.0)
                .sum();

        if (comprador.getSaldoTokens() < totalNecessario) {
            throw new RuntimeException(String.format(
                    "Saldo insuficiente. Necessário: T$ %.2f | Disponível: T$ %.2f",
                    totalNecessario, comprador.getSaldoTokens()));
        }

        // 4. Executa as compras
        List<CarrinhoCompraResponseDTO.ItemResultado> comprados = new ArrayList<>();
        double totalGasto = 0.0;

        for (Livro livro : livrosEncontrados) {
            try {
                Double preco = livro.getPrecoAprovado();
                if (preco == null) {
                    falhas.add(CarrinhoCompraResponseDTO.ItemResultado.builder()
                            .livroId(livro.getId())
                            .titulo(livro.getTitulo())
                            .motivo("Livro sem preço definido.")
                            .build());
                    continue;
                }

                // Registra pedido ANTES de deletar o livro
                pedidoService.registrarPedido(comprador, livro);

                comprador.setSaldoTokens(comprador.getSaldoTokens() - preco);
                totalGasto += preco;

                livroRepository.delete(livro);

                comprados.add(CarrinhoCompraResponseDTO.ItemResultado.builder()
                        .livroId(livro.getId())
                        .titulo(livro.getTitulo())
                        .preco(preco)
                        .build());

                // XP por compra
                gamificacaoService.xpCompra(comprador.getId());

            } catch (Exception e) {
                falhas.add(CarrinhoCompraResponseDTO.ItemResultado.builder()
                        .livroId(livro.getId())
                        .titulo(livro.getTitulo())
                        .motivo("Erro ao processar: " + e.getMessage())
                        .build());
            }
        }

        // 5. Persiste saldo atualizado e loga
        clienteRepository.save(comprador);

        logAuditoria.registrarLog(
                "COMPRA_CARRINHO",
                comprador.getId(),
                comprador.getEmail(),
                String.format("%d livro(s) comprado(s), %d falha(s), T$ %.2f debitados.",
                        comprados.size(), falhas.size(), totalGasto));

        // E-mail de confirmação do carrinho ao comprador
        if (!comprados.isEmpty()) {
            try {
                List<String[]> itensHtml = new java.util.ArrayList<>();
                comprados.forEach(item -> itensHtml.add(
                        new String[]{ item.getTitulo(), String.format("%.2f", item.getPreco()) }));
                emailService.enviarHtml(
                        comprador.getEmail(),
                        "Compra do carrinho confirmada! — Bibliotroca",
                        EmailHtmlBuilder.carrinhoConfirmado(comprador.getNome(), itensHtml, totalGasto,
                                comprador.getSaldoTokens(), baseUrl));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de carrinho para {}: {}", comprador.getEmail(), e.getMessage());
            }
        }

        return CarrinhoCompraResponseDTO.builder()
                .totalSolicitados(ids.size())
                .totalComprados(comprados.size())
                .totalGasto(totalGasto)
                .saldoRestante(comprador.getSaldoTokens())
                .comprados(comprados)
                .falhas(falhas)
                .build();
    }
}
