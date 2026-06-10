package umc.exs.service.core.livros.delegado;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.extern.GoogleBookData;
import umc.exs.dto.request.compra.LoteRequest;
import umc.exs.dto.request.livro.LivroItemRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.livro.ObraRpository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.api.ExternApi;
import umc.exs.service.core.dashboard.LoteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAnuncioService {

    private static final String URL_UPLOAD = "uploads/livros/";

    private static final String LOG_LIVRO_CADASTRADO = "LIVRO_ANUNCIO_CADASTRADO";
    private static final String LOG_LOTE_CADASTRADO = "LOTE_CADASTRO_CRIADO";
    private static final String LOG_LIVRO_ISBN_CRIADO = "LIVRO_ISBN_PROCESSADO";
    private static final String LOG_PROMO_LISTAGEM = "LIVRO_PROMOCOES_ATIVAS_LISTADAS";
    private static final String LOG_LIVRO_BUSCA_ATIVA = "LIVRO_BUSCA_ATIVO";

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final ObraRpository obraRepository;

    private final LogAuditoriaService logAuditoria;
    private final LoteService loteService;
    private final ExternApi googleBooksService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================= VENDA INDIVIDUAL =========================

    @Transactional
    public Livro cadastrarVenda(String email, LivroRequest dto, MultipartFile foto) {

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("Foto obrigatória para anúncio individual");
        }

        Cliente vendedor = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Cliente não localizado"));

        String urlFoto = salvarFoto(foto);
        String jsonFotos = converterParaJson(List.of(urlFoto));

        GoogleBookData response = googleBooksService.buscarPorIsbnAsync(dto.getIsbn()).join();

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("ISBN não encontrado na API externa: " + dto.getIsbn());
        }

        var info = response.getItems().get(0).getVolumeInfo();

        String autor = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                ? info.getAuthors().get(0)
                : "Autor Desconhecido";

        String capaUrl = (info.getImageLinks() != null)
                ? info.getImageLinks().getThumbnail()
                : null;

        Obra obra = obterOuCriarObra(info.getTitle(), autor, info.getLanguage(), capaUrl);

        Livro anuncio = Livro.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .isbn(dto.getIsbn())
                .fotosUrls(jsonFotos)
                .vendedor(vendedor)
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .obra(obra)
                .build();

        Livro salvo = livroRepository.save(anuncio);

        logAuditoria.registrarLog(
                LOG_LIVRO_CADASTRADO,
                vendedor.getId(),
                vendedor.getEmail(),
                "livroId=" + salvo.getId() + ", status=PENDENTE_APROVACAO"
        );

        return salvo;
    }

    // ========================= LOTE =========================

    @Transactional
    public Lote criarLote(String email, LoteRequest dto, List<MultipartFile> fotos) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Cliente não encontrado"));

        if (loteService.countPendingByCliente(cliente.getId()) >= 5) {
            throw new IllegalStateException("Limite de lotes pendentes atingido");
        }

        Lote lote = Lote.builder()
                .cliente(cliente)
                .codigoProtocolo(UUID.randomUUID().toString())
                .dataCriacao(LocalDateTime.now())
                .status(Lote.LoteStatus.PENDENTE)
                .build();

        loteRepository.save(lote);

        int fotoIndex = 0;

        for (LivroItemRequest item : dto.getLivros()) {

            List<String> urls = new ArrayList<>();

            int fotosPorLivro = calcularFotosPorLivro(item, dto, fotos);

            for (int i = 0; i < fotosPorLivro && fotoIndex < fotos.size(); i++) {

                MultipartFile foto = fotos.get(fotoIndex++);

                if (foto != null && !foto.isEmpty()) {
                    urls.add(salvarFoto(foto));
                }
            }

            String jsonFotos = converterParaJson(urls);

            Livro anuncio = Livro.builder()
                    .titulo(item.getTitulo())
                    .autor(item.getAutor())
                    .isbn(item.getIsbn())
                    .fotosUrls(jsonFotos)
                    .vendedor(cliente)
                    .lote(lote)
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .build();

            livroRepository.save(anuncio);
        }

        logAuditoria.registrarLog(
                LOG_LOTE_CADASTRADO,
                cliente.getId(),
                cliente.getEmail(),
                "loteId=" + lote.getId() + ", status=PENDENTE"
        );

        return lote;
    }

    // ========================= ISBN AUTO =========================

    @Transactional
    public Livro cadastrarPorIsbn(String isbn) {

        GoogleBookData response = googleBooksService.buscarPorIsbnAsync(isbn).join();

        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {

            var info = response.getItems().get(0).getVolumeInfo();

            String autor = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                    ? info.getAuthors().get(0)
                    : "Autor Desconhecido";

            Livro livro = Livro.builder()
                    .titulo(info.getTitle())
                    .autor(autor)
                    .isbn(isbn)
                    .idioma(info.getLanguage())
                    .resumoOficial(info.getDescription())
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .build();

            logAuditoria.registrarLog(
                    LOG_LIVRO_ISBN_CRIADO,
                    "isbn=" + isbn + ", origem=GOOGLE_BOOKS"
            );

            return livro;
        }

        log.info("Google Books falhou para ISBN {}. Tentando fallback OpenLibrary...", isbn);

        var fallback = googleBooksService.buscarPorIsbnOpenLibrary(isbn);

        if (fallback.isPresent()) {
            return fallback.get();
        }

        throw new EntityNotFoundException("Livro não encontrado automaticamente");
    }

    // ========================= PROMO LISTAGEM =========================

    public List<Livro> listarPromocoesAtivas() {

        List<Livro> livros = livroRepository.findPromocoesAtivas(LocalDateTime.now());

        List<Livro> response = new ArrayList<>();

        for (Livro livro : livros) {
            Livro dto = new Livro();
            dto.setId(livro.getId());
            dto.setTitulo(livro.getTitulo());
            dto.setAutor(livro.getAutor());
            dto.setIsbn(livro.getIsbn());
            dto.setPrecoAprovado(livro.getPrecoAprovado());
            response.add(dto);
        }
        return response;
    }

    public Livro buscarPorIdAtivo(Long id) {

        Livro livro = livroRepository.findByIdAndAprovadoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Livro não encontrado"));

        return livro;
    }

    // ========================= HELPERS =========================

    private String salvarFoto(MultipartFile foto) {
        String nomeOriginal = foto.getOriginalFilename();
        String nomeSanitizado = sanitizarNomeArquivo(nomeOriginal);
        String nome = UUID.randomUUID() + "_" + nomeSanitizado;

        Path caminho = Paths.get(URL_UPLOAD + nome);

        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
            return "/" + URL_UPLOAD + nome;
        } catch (IOException e) {
            log.error("UPLOAD_FALHA file={} erro={}", nome, e.getMessage(), e);
            throw new IllegalStateException("Erro ao salvar foto");
        }
    }

    private String sanitizarNomeArquivo(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) return "imagem.jpg";

        String base = Paths.get(nomeOriginal).getFileName().toString();
        String safe = base.replaceAll("[^a-zA-Z0-9.\\-_]", "_");

        return safe.isBlank() ? "imagem.jpg" : safe;
    }

    private String converterParaJson(List<String> lista) {
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private int calcularFotosPorLivro(LivroItemRequest item, LoteRequest dto, List<MultipartFile> fotos) {

        int quantidade = item.getQuantidadedeFotos();

        if (quantidade == 0 && !dto.getLivros().isEmpty()) {
            quantidade = fotos.size() / dto.getLivros().size();
        }

        return quantidade;
    }

    private Obra obterOuCriarObra(String titulo, String autor, String idioma, String capa) {

        return obraRepository.findByTituloAndAutor(titulo, autor)
                .orElseGet(() -> obraRepository.save(
                        Obra.builder()
                                .titulo(titulo)
                                .autor(autor)
                                .idioma(idioma)
                                .imageLinksJson(capa)
                                .build()
                ));
    }
}