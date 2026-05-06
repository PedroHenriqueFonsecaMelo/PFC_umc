package umc.exs.service.core.bussiness;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.compra.LoteRequestDTO;
import umc.exs.DTOs.livro.LivroItemDTO;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.LoteService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAnuncioService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    
    private final LogAuditoriaService logAuditoria;
    private final LoteService loteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
                    .dataAnuncio(LocalDateTime.now())
                    .aprovado(false)
                    .build();

            livroRepository.save(anuncio);

        }

        logAuditoria.registrarLog("LOTE_CADASTRADO", cliente.getId(), cliente.getEmail(),
                "Lote " + lote.getId() + " - aguardando aprovação");

        return lote;

    }
}
