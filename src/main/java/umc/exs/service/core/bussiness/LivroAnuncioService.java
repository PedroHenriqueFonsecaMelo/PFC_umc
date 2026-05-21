package umc.exs.service.core.bussiness;

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
import umc.exs.service.core.control.LoteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAnuncioService {

    /** Caminho relativo ao working-directory do processo (sem / inicial). */
    private static final String URL_UPLOAD = "uploads/livros/";

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final ObraRpository obraRepository;

    private final LogAuditoriaService logAuditoria;
    private final LoteService loteService;
    private final ExternApi googleBooksService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("null")
    @Transactional
    public Livro cadastrarVenda(String email, LivroRequest dto, MultipartFile foto) {

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("A foto é obrigatória para venda individual");
        }

        Cliente vendedor = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Cliente não localizado"));

        String urlFoto = salvarFoto(foto);

        String jsonFotos = converterParaJson(List.of(urlFoto));

        GoogleBookData response = googleBooksService.buscarPorIsbnAsync(dto.getIsbn()).join();

        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("Livro não encontrado na API externa para o ISBN: " + dto.getIsbn());
        }

        var info = response.getItems().get(0).getVolumeInfo();

        String primeiroAutor = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                ? info.getAuthors().get(0)
                : "Autor Desconhecido";

        String capaUrl = "";

        if (info.getImageLinks() != null &&
                info.getImageLinks().getThumbnail() != null) {

            capaUrl = info.getImageLinks().getThumbnail();
        }

        Obra obra = obterOuCriarObra(info.getTitle(), primeiroAutor,
                info.getLanguage(), capaUrl);

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

        logAuditoria.registrarLog("LIVRO_CADASTRADO",
                vendedor.getId(),
                vendedor.getEmail(),
                "Livro " + salvo.getId() + " - aguardando aprovação");

        return salvo;
    }

    @SuppressWarnings("null")
    @Transactional
    public Lote criarLote(String email, LoteRequest dto, List<MultipartFile> fotos) {

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Cliente não encontrado"));

        if (loteService.countPendingByCliente(cliente.getId()) >= 5) {
            throw new IllegalStateException("Limite de 5 lotes pendentes atingido");
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

        logAuditoria.registrarLog("LOTE_CADASTRADO",
                cliente.getId(),
                cliente.getEmail(),
                "Lote " + lote.getId() + " - aguardando aprovação");

        return lote;
    }

    @SuppressWarnings("null")
    @Transactional
    public Livro cadastrarPorIsbn(String isbn) {
        // 1. Tenta Google Books (nunca lança exceção — retorna null se indisponível)
        GoogleBookData response = googleBooksService.buscarPorIsbnAsync(isbn).join();

        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            var info = response.getItems().get(0).getVolumeInfo();

            String primeiroAutor = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                    ? info.getAuthors().get(0)
                    : "Autor Desconhecido";

            return Livro.builder()
                    .titulo(info.getTitle())
                    .autor(primeiroAutor)
                    .isbn(isbn)
                    .idioma(info.getLanguage())
                    .resumoOficial(info.getDescription())
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .obra(null)
                    .build();
        }

        // 2. Fallback: OpenLibrary
        log.info("Google Books não retornou dados para ISBN {}. Tentando OpenLibrary...", isbn);
        var openLibraryResult = googleBooksService.buscarPorIsbnOpenLibrary(isbn);
        if (openLibraryResult.isPresent()) {
            return openLibraryResult.get();
        }

        // 3. Ambas as APIs falharam — retorna 404 com mensagem amigável
        throw new EntityNotFoundException(
                "Livro não encontrado automaticamente. Preencha os dados manualmente.");
    }

    // ========================= MÉTODOS AUXILIARES =========================

    private String salvarFoto(MultipartFile foto) {
        String nomeOriginal = foto.getOriginalFilename();

        // Sanitiza o nome: mantém apenas alfanuméricos, hífens, underscores e ponto.
        // Também garante extensão ".jpg" quando o nome está ausente ou é vazio.
        String nomeSanitizado = sanitizarNomeArquivo(nomeOriginal);
        String nome = UUID.randomUUID() + "_" + nomeSanitizado;

        // URL_UPLOAD é relativo ao working-directory — nunca começa com /
        Path caminho = Paths.get(URL_UPLOAD + nome);

        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
            // A URL pública inicia com / para ser acessível pelo browser
            return "/" + URL_UPLOAD + nome;
        } catch (IOException e) {
            log.error("Erro de I/O ao salvar foto '{}': {}", nome, e.getMessage(), e);
            throw new IllegalStateException("Erro ao salvar foto: " + nome + " — " + e.getMessage());
        }
    }

    /**
     * Remove caracteres perigosos do nome de arquivo enviado pelo cliente.
     * - Strips separadores de caminho (path traversal)
     * - Mantém: letras, dígitos, hífen, underscore, ponto
     * - Se o resultado ficar vazio ou sem extensão válida, usa "imagem.jpg"
     */
    private String sanitizarNomeArquivo(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "imagem.jpg";
        }
        // Remove qualquer componente de caminho (ex: ../../etc/passwd)
        String base = Paths.get(nomeOriginal).getFileName().toString();
        // Mantém apenas chars seguros
        String seguro = base.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
        // Garante que tem pelo menos algum conteúdo
        return seguro.isBlank() ? "imagem.jpg" : seguro;
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
            log.info("Fotos por livro ajustado para: {}", quantidade);
        }

        return quantidade;
    }

    public List<Livro> listarPromocoesAtivas() {
        List<Livro> livros = livroRepository.findPromocoesAtivas(LocalDateTime.now());

        List<Livro> lista = new ArrayList<>();

        for (Livro livro : livros) {
            Livro dto = new Livro();

            dto.setId(livro.getId());
            dto.setTitulo(livro.getTitulo());
            dto.setAutor(livro.getAutor());
            dto.setIsbn(livro.getIsbn());
            dto.setPrecoAprovado(livro.getPrecoAprovado());

            lista.add(dto);
        }

        return lista;
    }

    public Livro buscarPorIdAtivo(Long id) {
        return livroRepository.findByIdAndAprovadoTrue(id)
                .orElseThrow(() -> new EntityNotFoundException("Livro não encontrado"));
    }

    @SuppressWarnings("null")
    private Obra obterOuCriarObra(String titulo, String autor, String idioma, String capa) {

        return obraRepository
                .findByTituloAndAutor(titulo, autor)
                .orElseGet(() -> {

                    Obra nova = Obra.builder()
                            .titulo(titulo)
                            .autor(autor)
                            .idioma(idioma)
                            .imageLinksJson(capa)
                            .build();

                    return obraRepository.save(nova);
                });
    }

}