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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.compra.lote.LoteRequestDTO;
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

    private static final String PATH_UPLOAD = "uploads/livros/";
    private static final String URL_UPLOAD = "/uploads/livros/";

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final ObraRpository obraRepository;

    private final LogAuditoriaService logAuditoria;
    private final LoteService loteService;

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

        Obra obra = obterOuCriarObra(dto.getTitulo(), dto.getAutor());
        
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

            Livro anuncio = Livro.builder()
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

        logAuditoria.registrarLog("LOTE_CADASTRADO",
                cliente.getId(),
                cliente.getEmail(),
                "Lote " + lote.getId() + " - aguardando aprovação");

        return lote;
    }

    // ========================= MÉTODOS AUXILIARES =========================

    private String salvarFoto(MultipartFile foto) {
        String nome = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path caminho = Paths.get(PATH_UPLOAD + nome);

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

    @SuppressWarnings("null")
    private Obra obterOuCriarObra(String titulo, String autor) {

        return obraRepository
                .findByTituloAndAutor(titulo, autor)
                .orElseGet(() -> {

                    Obra nova = Obra.builder()
                            .titulo(titulo)
                            .autor(autor)
                            .build();

                    return obraRepository.save(nova);
                });
    }

}