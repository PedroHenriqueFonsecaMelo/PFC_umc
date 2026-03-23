package umc.exs.service.core;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.DTOs.compra.LoteRequestDTO;
import umc.exs.DTOs.livro.LivroItemDTO;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.LivroRepository;
import umc.exs.repository.LoteRepository;
import umc.exs.service.log.LogAuditoriaService;

import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final LogAuditoriaService logAuditoria;

    private static final double TOKEN_REWARD = 10.0;

    private final LoteService loteService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cria lote venda com múltiplos livros + fotos.
     * Salva imagens uploads/livros, JSON fotos.
     * Recompensa tokens por livro, log LOTE_CADASTRADO.
     * Limite 5 pendentes cliente.
     */
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

            int fotosPorLivro = 3;

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
                        } catch (IOException e) {
                            throw new RuntimeException("Erro ao salvar foto: " + foto.getOriginalFilename());
                        }
                    }
                    fotoIndex++;
                }
            }

            String jsonFotos = "[]";
            try {
                jsonFotos = objectMapper.writeValueAsString(bookFotosUrls);
            } catch (JsonProcessingException e) {
                jsonFotos = "[]";
            }

            LivroAnuncio anuncio = LivroAnuncio.builder()
                    .titulo(item.getTitulo())
                    .autor(item.getAutor())
                    .isbn(item.getIsbn())
                    .fotosUrls(jsonFotos)
                    .lote(lote)
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .build();

            livroRepository.save(anuncio);
        }

        // Atualiza saldo e logs...
        cliente.setSaldoTokens(cliente.getSaldoTokens() + TOKEN_REWARD * dto.getLivros().size());
        clienteRepository.save(cliente);

        logAuditoria.registrarLog("LOTE_CADASTRADO", cliente.getId(), cliente.getEmail(),
                "Lote " + lote.getId() + " com " + dto.getLivros().size() + " livros");

        return lote;
    }

    /**
     * Cadastra venda livro individual + foto.
     * Salva upload local, aprovado=false pendente.
     * Recompensa TOKEN_REWARD vendedor.
     */
    @Transactional
    public LivroAnuncio cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
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

        LivroAnuncio anuncio = LivroAnuncio.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .isbn(dto.getIsbn())
                .fotosUrls(jsonFotos)
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .build();

        LivroAnuncio salvo = livroRepository.save(anuncio);

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);

        if (clienteOpt.isEmpty()) {
            throw new RuntimeException("Cliente não localizado.");
        }

        Cliente vendedor = clienteOpt.get();

        if (vendedor != null) {
            vendedor.setSaldoTokens(vendedor.getSaldoTokens() + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            logAuditoria.registrarLog("LIVRO_CADASTRADO_RECOMPENSA", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + salvo.getId() + " T$" + TOKEN_REWARD);
        }

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
    public List<LivroAnuncio> listarLivrosPorLote(Long loteId) {
        return livroRepository.findByLoteIdAndAprovadoFalse(loteId);
    }

    /**
     * Livros aprovados vitrine.
     */
    public List<LivroAnuncio> listarLivrosAprovados() {
        return livroRepository.findByAprovadoTrue();
    }

    /**
     * Livros pendentes admin.
     */
    public List<LivroAnuncio> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    /**
     * Todos livros admin.
     */
    public List<LivroAnuncio> listarTodosLivros() {
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
    @Transactional
    public LivroAnuncio aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {
        LivroAnuncio anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());
        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());

        LivroAnuncio saved = livroRepository.save(anuncio);

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
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        LivroAnuncio anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        String emailAdmin = "admin@sistema.com";

        System.out.println("estado: " + estado);
        System.out.println("estado: " + EstadoLivro.RUIM.name());
        System.out.println("estado: " + EstadoLivro.RUIM.name().toString());


        if (!estado.equalsIgnoreCase(EstadoLivro.RUIM.name().toString())) {
            throw new RuntimeException("Apenas livros com estado RUIM ou pior podem ser rejeitados");
        }

        livroRepository.delete(anuncio);

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, emailAdmin,
                "Livro ID " + livroId + " rejeitado pelo administrador.");
    }

    /**
     * Processa compra livro aprovado.
     * Deduz tokens comprador, deleta anúncio.
     * Log COMPRA_LIVRO_SUCESSO.
     */
    @Transactional
    public void realizarCompra(Long livroId, String emailComprador) {
        LivroAnuncio anuncio = livroRepository.findByIdAndAprovadoTrue(livroId)
                .orElseThrow(() -> new RuntimeException("Anúncio não aprovado"));

        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado"));

        Double preco = anuncio.getPrecoAprovado();
        if (preco == null || comprador.getSaldoTokens() < preco) {
            throw new RuntimeException("Saldo insuficiente T$" + preco);
        }

        comprador.setSaldoTokens(comprador.getSaldoTokens() - preco);
        livroRepository.delete(anuncio);
        clienteRepository.save(comprador);

        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + preco);
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service core livros venda/anúncio aprovação/compra.
 * Cria lote individual, upload fotos, recompensa tokens.
 * Admin aprova/rejeita define preço enum EstadoLivro.
 * Compra deduz saldo deleta vitrine, logs auditoria.
 */
