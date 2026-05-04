package umc.exs.service.core.cliente;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.DTOs.compra.CarrinhoCompraRequestDTO;
import umc.exs.DTOs.compra.CarrinhoCompraResponseDTO;
import umc.exs.DTOs.compra.LoteRequestDTO;
import umc.exs.DTOs.livro.LivroItemDTO;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.ListaDesejosService;
import umc.exs.service.core.control.LoteService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;

import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final LogAuditoriaService logAuditoria;
    private final umc.exs.service.gamificacao.GamificacaoService gamificacaoService;
    private final PedidoService pedidoService;
    private final EmailService emailService;
    private final ListaDesejosService listaDesejosService;

    private static final double TOKEN_REWARD = 10.0;

    private final LoteService loteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cria lote venda com múltiplos livros + fotos.
     * Salva imagens uploads/livros, JSON fotos.
     * Recompensa tokens por livro APENAS NA APROVAÇÃO, log LOTE_CADASTRADO.
     * Limite 5 pendentes cliente.
     * * ALTERAÇÃO: Quantidade de fotos por livro é DINÂMICA
     * (item.getQuantidadedeFotos()).
     */
    @SuppressWarnings("null")
    @Transactional
    public Lote criarLote(String email, LoteRequestDTO dto, List<MultipartFile> fotos) {

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (clienteOpt.isEmpty()) {
            throw new RuntimeException("Cliente não encontrado");
        }
        Cliente cliente = clienteOpt.get();

        if (loteService.countPendingByCliente(cliente.getId()) >= 5) {
            throw new RuntimeException("Limite de 5 lotes pendentes atingido");
        }

        Lote lote = Lote.builder()
                .cliente(cliente)
                .codigoProtocolo(UUID.randomUUID().toString())
                .dataCriacao(LocalDateTime.now())
                .status(Lote.LoteStatus.PENDENTE)
                .build();
        loteRepository.save(lote);

        int fotoIndex = 0;
        for (LivroItemDTO item : dto.getLivros()) {
            List<String> bookFotosUrls = new ArrayList<>();

            int fotosPorLivro = item.getQuantidadedeFotos();
            if (fotosPorLivro == 0) {
                fotosPorLivro = fotos.size() / dto.getLivros().size();
                log.info("fotosPorLivro era 0, ajustado para: {}", fotosPorLivro);
            }
            log.info("Processando item: {}, fotosPorLivro: {}", item.getTitulo(), fotosPorLivro);

            for (int k = 0; k < fotosPorLivro; k++) {
                if (fotoIndex < fotos.size()) {
                    MultipartFile foto = fotos.get(fotoIndex);

                    if (foto != null && !foto.isEmpty()) {
                        String nomeFoto = UUID.randomUUID() + "_" + foto.getOriginalFilename();
                        Path caminho = Paths.get("uploads/livros/" + nomeFoto);
                        try {
                            Files.createDirectories(caminho.getParent());
                            Files.copy(foto.getInputStream(), caminho);
                            bookFotosUrls.add("/uploads/livros/" + nomeFoto);
                            log.info("Foto salva: {}", "/uploads/livros/" + nomeFoto);
                        } catch (IOException e) {
                            throw new RuntimeException("Erro ao salvar foto: " + foto.getOriginalFilename());
                        }
                    }
                    fotoIndex++;
                }
            }

            log.info("bookFotosUrls size após loop: {}", bookFotosUrls.size());
            String jsonFotos = "[]";
            try {
                if (!bookFotosUrls.isEmpty()) {
                    jsonFotos = objectMapper.writeValueAsString(bookFotosUrls);
                }
            } catch (JsonProcessingException e) {
                jsonFotos = "[]";
            }
            log.info("jsonFotos final: {}", jsonFotos);

            Livro anuncio = Livro.builder()
                    .titulo(item.getTitulo())
                    .autor(item.getAutor())
                    .isbn(item.getIsbn())
                    .fotosUrls(jsonFotos)
                    .lote(lote)
                    .vendedor(cliente)
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .build();

            livroRepository.save(anuncio);
        }

        logAuditoria.registrarLog("LOTE_CADASTRADO", cliente.getId(), cliente.getEmail(),
                "Lote " + lote.getId() + " - aguardando aprovação");

        return lote;
    }

    /**
     * Cadastra venda livro individual + foto.
     * Salva upload local, aprovado=false pendente.
     * Recompensa TOKEN_REWARD vendedor.
     */
    @SuppressWarnings("null")
    @Transactional
    public Livro cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
        if (foto == null || foto.isEmpty()) {
            throw new RuntimeException("A foto é obrigatória para venda individual");
        }

        String nomeFoto = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path caminho = Paths.get("uploads/livros/" + nomeFoto);
        String urlFinal = "/uploads/livros/" + nomeFoto;

        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar a foto");
        }

        // Criar o JSON para o campo fotosUrls contendo a foto única
        List<String> listaFotoUnica = List.of(urlFinal);
        String jsonFotos = "[]";
        try {
            jsonFotos = objectMapper.writeValueAsString(listaFotoUnica);
        } catch (JsonProcessingException e) {
            jsonFotos = "[\"" + urlFinal + "\"]";
        }

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);

        if (clienteOpt.isEmpty()) {
            throw new RuntimeException("Cliente não localizado.");
        }

        Cliente vendedor = clienteOpt.get();

        Livro anuncio = Livro.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .isbn(dto.getIsbn())
                .fotosUrls(jsonFotos)
                .vendedor(vendedor)
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .build();

        Livro salvo = livroRepository.save(anuncio);

        logAuditoria.registrarLog("LIVRO_CADASTRADO", vendedor.getId(), vendedor.getEmail(),
                "Livro " + salvo.getId() + " - aguardando aprovação");

        return salvo;
    }

    /**
     * Lista lotes pendentes aprovação.
     */
    public List<Lote> listarLotesPendentes() {
        return loteService.listarPendentes();
    }

    /**
     * Livros lote não aprovados.
     */
    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroRepository.findByLoteIdAndAprovadoFalse(loteId);
    }

    /**
     * Livros aprovados vitrine.
     */
    public List<Livro> listarLivrosAprovados() {
        return livroRepository.findByAprovadoTrue();
    }

    /**
     * Livros pendentes admin.
     */
    public List<Livro> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    /**
     * Todos livros admin.
     */
    public List<Livro> listarTodosLivros() {
        return livroRepository.findAll();
    }

    /**
     * Aprova livro admin define preço/estado.
     * Transfer system, update lote status se completo.
     * 
     * @param livroId ID
     * @param adminId aprovador
     * @param dto     aprovação
     */
    @SuppressWarnings("null")
    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());
        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isBlank()) {
            anuncio.setFotosUrls(dto.getFotosUrls());
        }

        Livro saved = livroRepository.save(anuncio);

        // Notificar clientes interessados via lista de desejos
        try {
            listaDesejosService.notificarClientesSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());
        } catch (Exception e) {
            log.error("Falha ao notificar lista de desejos para ISBN {}: {}", anuncio.getIsbn(), e.getMessage());
        }

        // Identificar o vendedor: campo direto ou via lote
        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            // Creditar tokens ao vendedor apenas na aprovação
            vendedor.setSaldoTokens(vendedor.getSaldoTokens() + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            // Gamificação: XP ao vendedor
            gamificacaoService.xpLivroAprovado(vendedor.getId());

            logAuditoria.registrarLog("LIVRO_APROVADO_RECOMPENSA", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + livroId + " aprovado - T$" + TOKEN_REWARD + " creditados");

            // E-mail de confirmação ao vendedor
            try {
                emailService.enviar(
                        vendedor.getEmail(),
                        "Seu livro foi aprovado!",
                        "Olá, " + vendedor.getNome() + "!\n\n" +
                                "Seu livro \"" + anuncio.getTitulo() + "\" foi aprovado e está disponível na vitrine.\n" +
                                "Você recebeu T$ " + TOKEN_REWARD + " como recompensa.\n\n" +
                                "Equipe Bookstore"
                );
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de aprovação para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }
        }

        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();
            long pendingCount = livroRepository.countPendingByLoteId(loteId);
            if (pendingCount == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return saved;
    }

    /**
     * Rejeita livro com comentário admin.
     * Set aprovado=false, comentarioAprovacao.
     * Log LIVRO_REJEITADO.
     */
    @SuppressWarnings("null")
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        if (!estado.equalsIgnoreCase(EstadoLivro.RUIM.name())) {
            throw new RuntimeException("Apenas livros com estado RUIM ou pior podem ser rejeitados");
        }

        // E-mail de rejeição ao vendedor (antes de deletar)
        Cliente vendedorRejeicao = anuncio.getVendedor();
        if (vendedorRejeicao == null && anuncio.getLote() != null) {
            vendedorRejeicao = anuncio.getLote().getCliente();
        }
        if (vendedorRejeicao != null) {
            final String emailVendedor = vendedorRejeicao.getEmail();
            final String nomeVendedor = vendedorRejeicao.getNome();
            final String tituloLivro = anuncio.getTitulo();
            try {
                emailService.enviar(
                        emailVendedor,
                        "Seu livro não foi aprovado",
                        "Olá, " + nomeVendedor + "!\n\n" +
                                "Infelizmente, o livro \"" + tituloLivro + "\" não foi aprovado.\n" +
                                (comentario != null && !comentario.isBlank()
                                        ? "Motivo: " + comentario + "\n\n"
                                        : "\n") +
                                "Você pode enviar novos livros para avaliação a qualquer momento.\n\n" +
                                "Equipe Bookstore"
                );
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de rejeição para vendedor {}: {}", emailVendedor, e.getMessage());
            }
        }

        livroRepository.delete(anuncio);

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin#" + adminId,
                "Livro ID " + livroId + " rejeitado pelo administrador.");
    }

    /**
     * Processa compra livro aprovado.
     * Registra pedido, deduz tokens, deleta anúncio.
     * Log COMPRA_LIVRO_SUCESSO.
     */
    @Transactional
    public void realizarCompra(Long livroId, String emailComprador) {
        Livro anuncio = livroRepository.findByIdAndAprovadoTrue(livroId)
                .orElseThrow(() -> new RuntimeException("Anúncio não aprovado"));

        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado"));

        Double preco = anuncio.getPrecoAprovado();
        if (preco == null || comprador.getSaldoTokens() < preco) {
            throw new RuntimeException("Saldo insuficiente T$" + preco);
        }

        // Registra pedido ANTES de deletar o livro
        pedidoService.registrarPedido(comprador, anuncio);

        comprador.setSaldoTokens(comprador.getSaldoTokens() - preco);
        livroRepository.delete(anuncio);
        clienteRepository.save(comprador);

        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + preco);

        // E-mail de confirmação de compra ao comprador
        try {
            emailService.enviar(
                    comprador.getEmail(),
                    "Compra realizada com sucesso!",
                    "Olá, " + comprador.getNome() + "!\n\n" +
                            "Sua compra do livro \"" + anuncio.getTitulo() + "\" foi confirmada.\n" +
                            "Valor debitado: T$ " + preco + "\n" +
                            "Saldo atual: T$ " + comprador.getSaldoTokens() + "\n\n" +
                            "Acompanhe o status do envio em 'Minhas Compras'.\n\n" +
                            "Equipe Bookstore"
            );
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de compra para {}: {}", comprador.getEmail(), e.getMessage());
        }

        // Gamificação: XP para o comprador
        gamificacaoService.xpCompra(comprador.getId());
    }

    /**
     * Processa compra em lote via carrinho.
     *
     * Estratégia:
     * 1. Valida se o comprador existe e não está bloqueado.
     * 2. Busca todos os livros solicitados de uma vez (evita N queries).
     * 3. Verifica saldo total antes de qualquer débito (Fail-Fast).
     * 4. Executa as compras individualmente dentro da mesma transação:
     * - se um livro não for encontrado ou já tiver sido comprado por
     * outro usuário (race condition), ele vai para a lista de falhas
     * sem abortar os demais.
     * 5. Persiste o novo saldo e loga a operação.
     *
     * @param emailComprador email do usuário autenticado
     * @param request        lista de IDs dos livros no carrinho
     * @return resumo com comprados, falhas e saldo restante
     */
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
                StringBuilder itensMsg = new StringBuilder();
                comprados.forEach(item -> itensMsg
                        .append("• ").append(item.getTitulo())
                        .append(" — T$ ").append(String.format("%.2f", item.getPreco()))
                        .append("\n"));
                emailService.enviar(
                        comprador.getEmail(),
                        "Compra do carrinho confirmada!",
                        "Olá, " + comprador.getNome() + "!\n\n" +
                                "Sua compra foi confirmada com sucesso.\n\n" +
                                "Itens adquiridos:\n" + itensMsg +
                                "\nTotal debitado: T$ " + String.format("%.2f", totalGasto) + "\n" +
                                "Saldo atual: T$ " + String.format("%.2f", comprador.getSaldoTokens()) + "\n\n" +
                                "Acompanhe o status dos envios em 'Minhas Compras'.\n\n" +
                                "Equipe Bookstore"
                );
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

    // ── CRUD Admin ────────────────────────────────────────────────

    public Livro adicionarLivroAdmin(String titulo, String autor, String isbn,
                                     Double preco, EstadoLivro estado, String resumo, Long adminId) {
        Livro livro = Livro.builder()
                .titulo(titulo)
                .autor(autor)
                .isbn(isbn != null ? isbn : "")
                .precoAprovado(preco)
                .estadoAprovado(estado)
                .resumoOficial(resumo)
                .aprovado(true)
                .fotosUrls("[]")
                .dataAnuncio(LocalDateTime.now())
                .dataAprovacao(LocalDateTime.now())
                .adminAprovadorId(adminId)
                .build();
        Livro salvo = livroRepository.save(livro);
        logAuditoria.registrarLog("ADMIN_LIVRO_ADICIONADO", adminId, "admin",
                "Livro adicionado: " + titulo + " (ID " + salvo.getId() + ")");
        return salvo;
    }

    @Transactional
    public Livro editarLivroAdmin(Long id, String titulo, String autor, String isbn,
                                  Double preco, EstadoLivro estado, String resumo) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado."));
        if (titulo != null && !titulo.isBlank())  livro.setTitulo(titulo);
        if (autor  != null && !autor.isBlank())   livro.setAutor(autor);
        if (isbn   != null && !isbn.isBlank())    livro.setIsbn(isbn);
        if (preco  != null)                       livro.setPrecoAprovado(preco);
        if (estado != null)                       livro.setEstadoAprovado(estado);
        if (resumo != null)                       livro.setResumoOficial(resumo);
        return livroRepository.save(livro);
    }

    public void deletarLivroAdmin(Long id) {
        if (!livroRepository.existsById(id))
            throw new RuntimeException("Livro não encontrado.");
        livroRepository.deleteById(id);
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service core livros venda/anúncio aprovação/compra.
 * Cria lote individual, upload fotos, recompensa tokens.
 * Admin aprova/rejeita define preço enum EstadoLivro.
 * Compra deduz saldo deleta vitrine, logs auditoria.
 */
