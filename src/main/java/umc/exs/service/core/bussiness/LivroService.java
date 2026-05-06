package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.DTOs.compra.CarrinhoCompraRequestDTO;
import umc.exs.DTOs.compra.CarrinhoCompraResponseDTO;
import umc.exs.DTOs.compra.LoteRequestDTO;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.ArquivosService;
import umc.exs.service.core.control.PedidoService;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.core.control.LoteService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    private final EmailService emailService;
    private final PedidoService pedidoService;
    private final GamificacaoService gamificacaoService;
    private final LivroAnuncioService anuncioService;
    private final LivroAdminService adminService;
    private final LivroCompraService compraService;
    private final LoteService loteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final double TOKEN_REWARD_VENDEDOR = 10.0;

    @SuppressWarnings("null")
    @Transactional
    public Livro cadastrarAnuncio(String email, LivroRequestDTO dto, MultipartFile foto) {
        Cliente vendedor = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));

        String urlFoto = ArquivosService.salvarArquivoFisico(foto);
        String jsonFotos = converterParaJson(List.of(urlFoto));

        Livro anuncio = Livro.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .isbn(dto.getIsbn())
                .fotosUrls(jsonFotos)
                .vendedor(vendedor)
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .build();

        logAuditoria.registrarLog("LIVRO_CADASTRADO", vendedor.getId(), vendedor.getEmail(), "Aguardando Sistema");
        return livroRepository.save(anuncio);
    }

@Transactional
    public void realizarCompra(Long livroId, String emailComprador) {
        // O Lock deve ser a primeira coisa a acontecer
        Livro livro = livroRepository.findByIdAndAprovadoTrueWithLock(livroId)
                .orElseThrow(() -> new RuntimeException("Livro indisponível no sistema"));

        Cliente comprador = clienteRepository.findByEmail(emailComprador)
                .orElseThrow(() -> new RuntimeException("Comprador não encontrado"));

        Double preco = livro.getPrecoAprovado();
        if (preco == null || comprador.getSaldoTokens() < preco) {
            throw new RuntimeException("Saldo insuficiente T$" + preco);
        }

        comprador.setSaldoTokens(comprador.getSaldoTokens() - preco);

        pedidoService.registrarPedido(comprador, livro);

        clienteRepository.saveAndFlush(comprador);
        livroRepository.delete(livro);
        livroRepository.flush();

        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Livro " + livroId + " T$" + preco);

        // E-mail de confirmação de compra ao comprador
        try {
            emailService.enviar(
                    comprador.getEmail(),
                    "Compra realizada com sucesso!",
                    "Olá, " + comprador.getNome() + "!\n\n" +
                            "Sua compra do livro \"" + livro.getTitulo() + "\" foi confirmada.\n" +
                            "Valor debitado: T$ " + preco + "\n" +
                            "Saldo atual: T$ " + comprador.getSaldoTokens() + "\n\n" +
                            "Acompanhe o status do envio em 'Minhas Compras'.\n\n" +
                            "Equipe Bookstore"
            );
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de compra para {}: {}", comprador.getEmail(), e.getMessage());
        }

        gamificacaoService.xpCompra(comprador.getId());
    }

    @Transactional
    public Livro aprovarPeloSistema(@NonNull Long livroId, Long adminId, AdminAprovacaoDTO dto) {
        Livro livro = livroRepository.findById(livroId).orElseThrow();

        livro.setAprovado(true);
        livro.setPrecoAprovado((double) dto.getEstadoAprovado().getPreco());
        livro.setDataAprovacao(LocalDateTime.now());

        Cliente vendedor = livro.getVendedor();
        if (vendedor != null) {
            vendedor.setSaldoTokens(vendedor.getSaldoTokens() + TOKEN_REWARD_VENDEDOR);
            clienteRepository.save(vendedor);
            gamificacaoService.xpLivroAprovado(vendedor.getId());
        }

        return livroRepository.save(livro);
    }



    private String converterParaJson(List<String> urls) {
        try {
            return objectMapper.writeValueAsString(urls);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    // Delegando para os serviços especializados
    public Lote criarLote(String email, LoteRequestDTO dto, List<MultipartFile> fotos) {
        return anuncioService.criarLote(email, dto, fotos);
    }

    public Livro cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
        return anuncioService.cadastrarVenda(email, dto, foto);
    }

    public List<Livro> listarLivrosPorLote(Long loteId) {
        return adminService.listarLivrosPorLote(loteId);
    }

    public Livro aprovarLivro(@lombok.NonNull Long livroId, Long adminId, AdminAprovacaoDTO dto) {
        return adminService.aprovarLivro(livroId, adminId, dto);
    }

    public void rejeitarLivro(@lombok.NonNull Long livroId, Long adminId, String motivo, String observacao) {
        adminService.rejeitarLivro(livroId, adminId, motivo, observacao);
    }

    public Livro adicionarLivroAdmin(String titulo, String autor, String isbn, Double preco, EstadoLivro estado,
            String capa, @NonNull Long vendedorId) {
        return adminService.adicionarLivroAdmin(titulo, autor, isbn, preco, estado, capa, vendedorId);
    }

    public Livro editarLivroAdmin(@lombok.NonNull Long id, String titulo, String autor, String isbn, Double preco,
            EstadoLivro estado,
            String capa) {
        return adminService.editarLivroAdmin(id, titulo, autor, isbn, preco, estado, capa);
    }

    public void deletarLivroAdmin(@lombok.NonNull Long id) {
        adminService.deletarLivroAdmin(id);
    }

    /**
     * Lista lotes pendentes aprovação.
     */
    public List<Lote> listarLotesPendentes() {
        return loteService.listarPendentes();
    }

    /**
     * Livros pendentes admin.
     */
    public List<Livro> listarLivrosPendentes() {
        return adminService.listarLivrosPendentes();
    }

    /**
     * Todos livros admin.
     */
    public List<Livro> listarTodosLivros() {
        return livroRepository.findAll();
    }

    /**
     * Livros aprovados vitrine.
     */
    public List<Livro> listarLivrosAprovados() {
        return livroRepository.findByAprovadoTrue();
    }

    public CarrinhoCompraResponseDTO comprarCarrinho(String email, CarrinhoCompraRequestDTO dto) {
        return compraService.comprarCarrinho(email, dto);
    }
}

