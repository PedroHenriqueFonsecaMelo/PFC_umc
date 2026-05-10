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
import umc.exs.dtos.compra.lote.LoteRequestDTO;
import umc.exs.dtos.livro.GoogleBookResponse;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.dtos.livro.LivroItemDTO;
import umc.exs.dtos.livro.LivroRequestDTO;
import umc.exs.mappers.LivroMapper;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.livro.ObraRpository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.LoteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAnuncioService {

    private static final String URL_UPLOAD = "/uploads/livros/";

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final ObraRpository obraRepository;

    private final LogAuditoriaService logAuditoria;
    private final LoteService loteService;
    private final GoogleBooksService googleBooksService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LivroMapper livroMapper;

    @SuppressWarnings("null")
    @Transactional
    public LivroDTO cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {

        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("A foto é obrigatória para venda individual");
        }

        Cliente vendedor = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Cliente não localizado"));

        String urlFoto = salvarFoto(foto);

        String jsonFotos = converterParaJson(List.of(urlFoto));

        GoogleBookResponse response = googleBooksService.buscarPorIsbnAsync(dto.getIsbn()).join();

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

        return livroMapper.paraDTO(salvo);
    }

    @SuppressWarnings("null")
    @Transactional
    public Lote criarLote(String email, LoteRequestDTO dto, List<MultipartFile> fotos) {

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

        for (LivroItemDTO item : dto.getLivros()) {

            List<String> urls = new ArrayList<>();

            int fotosPorLivro = calcularFotosPorLivro(item, dto, fotos);

            for (int i = 0; i < fotosPorLivro && fotoIndex < fotos.size(); i++) {

                MultipartFile foto = fotos.get(fotoIndex++);

                if (foto != null && !foto.isEmpty()) {
                    urls.add(salvarFoto(foto));
                }
            }

            String jsonFotos = converterParaJson(urls);

            GoogleBookResponse response = googleBooksService.buscarPorIsbnAsync(item.getIsbn()).join();

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                throw new IllegalArgumentException("Livro não encontrado na API externa para o ISBN: " + item.getIsbn());
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
                    .titulo(item.getTitulo())
                    .autor(item.getAutor())
                    .isbn(item.getIsbn())
                    .fotosUrls(jsonFotos)
                    .lote(lote)
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .obra(obra)
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
    public LivroDTO cadastrarPorIsbn(String isbn) {
        // 1. Chama a API e TRAVA a execução até o Google responder (.join())
        GoogleBookResponse response = googleBooksService.buscarPorIsbnAsync(isbn).join();

        // 2. Valida se a API trouxe algo
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new IllegalArgumentException("Livro não encontrado na API externa para o ISBN: " + isbn);
        }

        var info = response.getItems().get(0).getVolumeInfo();

        // 3. Cria ou recupera a Obra (usando seu método já existente)
        String primeiroAutor = (info.getAuthors() != null && !info.getAuthors().isEmpty())
                ? info.getAuthors().get(0)
                : "Autor Desconhecido";

        // 4. Monta a entidade Livro com os dados da API
        Livro anuncio = Livro.builder()
                .titulo(info.getTitle())
                .autor(primeiroAutor)
                .isbn(isbn)
                .idioma(info.getLanguage())
                .resumoOficial(info.getDescription())
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .obra(null)
                .build();

        return livroMapper.paraDTO(anuncio);
    }

    // ========================= MÉTODOS AUXILIARES =========================

    private String salvarFoto(MultipartFile foto) {
        String nome = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path caminho = Paths.get(URL_UPLOAD + nome);

        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
            return URL_UPLOAD + nome;
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao salvar foto: " + nome);
        }
    }

    private String converterParaJson(List<String> lista) {
        try {
            return objectMapper.writeValueAsString(lista);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private int calcularFotosPorLivro(LivroItemDTO item, LoteRequestDTO dto, List<MultipartFile> fotos) {

        int quantidade = item.getQuantidadedeFotos();

        if (quantidade == 0 && !dto.getLivros().isEmpty()) {
            quantidade = fotos.size() / dto.getLivros().size();
            log.info("Fotos por livro ajustado para: {}", quantidade);
        }

        return quantidade;
    }

    public List<LivroDTO> listarPromocoesAtivas() {
        List<Livro> livros = livroRepository.findPromocoesAtivas(LocalDateTime.now());

        List<LivroDTO> lista = new ArrayList<>();

        for (Livro livro : livros) {
            LivroDTO dto = new LivroDTO();

            dto.setId(livro.getId());
            dto.setTitulo(livro.getTitulo());
            dto.setAutor(livro.getAutor());
            dto.setIsbn(livro.getIsbn());
            dto.setPrecoAprovado(livro.getPrecoAprovado());

            lista.add(dto);
        }

        return lista;
    }

    public LivroDTO buscarPorIdAtivo(Long id) {
        return livroRepository.findByIdAndAprovadoTrue(id)
                .map(livro -> livroMapper.paraDTO(livro))
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