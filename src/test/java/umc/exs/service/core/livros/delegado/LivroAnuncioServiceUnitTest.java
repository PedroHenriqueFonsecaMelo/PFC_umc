package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.dto.extern.GoogleBookData;
import umc.exs.dto.request.livro.LivroItemRequest;
import umc.exs.dto.request.livro.LivroRequest;
import umc.exs.dto.request.compra.LoteRequest;
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

@ExtendWith(MockitoExtension.class)
class LivroAnuncioServiceUnitTest {

        @Mock
        LivroRepository livroRepository;

        @Mock
        ClienteRepository clienteRepository;

        @Mock
        LoteRepository loteRepository;

        @Mock
        ObraRpository obraRepository;

        @Mock
        LogAuditoriaService logAuditoria;

        @Mock
        LoteService loteService;

        @Mock
        ExternApi googleBooksService;

        @InjectMocks
        LivroAnuncioService service;

        @Test
        void cadastrarVenda_quandoFotoNula_deveLancarIllegalArgumentException() {
                LivroRequest dto = new LivroRequest();
                dto.setTitulo("T");
                dto.setAutor("A");
                dto.setIsbn("ISBN");

                assertThrows(IllegalArgumentException.class,
                                () -> service.cadastrarVenda("user@email.com", dto, null));
        }

        @Test
        void cadastrarVenda_quandoFotoVazia_deveLancarIllegalArgumentException() {
                LivroRequest dto = new LivroRequest();
                dto.setTitulo("T");
                dto.setAutor("A");
                dto.setIsbn("ISBN");

                MultipartFile empty = mock(MultipartFile.class);
                when(empty.isEmpty()).thenReturn(true);

                assertThrows(IllegalArgumentException.class,
                                () -> service.cadastrarVenda("user@email.com", dto, empty));
        }

        @Test
        void cadastrarVenda_quandoClienteNaoEncontrado_deveLancarIllegalStateException() {

                LivroRequest dto = new LivroRequest();
                dto.setTitulo("T");
                dto.setAutor("A");
                dto.setIsbn("ISBN");

                MultipartFile f = mock(MultipartFile.class);

                when(clienteRepository.findByEmail("user@email.com"))
                                .thenReturn(Optional.empty());

                assertThrows(IllegalStateException.class,
                                () -> service.cadastrarVenda("user@email.com", dto, f));

                verify(livroRepository, never()).save(any());
        }

        @Test
        void cadastrarVenda_quandoIsbnNaoEncontrado_deveLancarIllegalArgumentException() throws IOException {

                LivroRequest dto = new LivroRequest();
                dto.setTitulo("T");
                dto.setAutor("A");
                dto.setIsbn("ISBN");

                Cliente vendedor = new Cliente();
                vendedor.setId(1L);
                vendedor.setEmail("user@email.com");

                MultipartFile f = mock(MultipartFile.class);
                when(f.isEmpty()).thenReturn(false);
                when(f.getOriginalFilename()).thenReturn("capa.jpg");
                when(f.getInputStream()).thenReturn(
                                new ByteArrayInputStream(new byte[] {
                                                (byte) 0xFF,
                                                (byte) 0xD8,
                                                (byte) 0xFF,
                                                0
                                }));

                when(clienteRepository.findByEmail("user@email.com"))
                                .thenReturn(Optional.of(vendedor));

                when(googleBooksService.buscarPorIsbnAsync("ISBN"))
                                .thenReturn(CompletableFuture.completedFuture(new GoogleBookData()));

                assertThrows(IllegalArgumentException.class,
                                () -> service.cadastrarVenda("user@email.com", dto, f));
        }

        @Test
        void cadastrarVenda_quandoSucesso_deveSalvarLivro() throws IOException {

                LivroRequest dto = new LivroRequest();
                dto.setTitulo("Titulo");
                dto.setAutor("AutorDTO");
                dto.setIsbn("ISBN");

                Cliente vendedor = new Cliente();
                vendedor.setId(1L);
                vendedor.setEmail("user@email.com");

                MultipartFile f = mock(MultipartFile.class);
                when(f.isEmpty()).thenReturn(false);
                when(f.getOriginalFilename()).thenReturn("capa.jpg");
                when(f.getInputStream()).thenReturn(
                                new ByteArrayInputStream(new byte[] {
                                                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0
                                }));

                when(clienteRepository.findByEmail("user@email.com"))
                                .thenReturn(Optional.of(vendedor));

                GoogleBookData resp = new GoogleBookData();

                GoogleBookData.Item item = new GoogleBookData.Item();

                GoogleBookData.VolumeInfo volumeInfo = new GoogleBookData.VolumeInfo();
                volumeInfo.setTitle("Titulo");
                volumeInfo.setAuthors(List.of("AutorDTO"));

                item.setVolumeInfo(volumeInfo);

                resp.setItems(List.of(item));

                when(googleBooksService.buscarPorIsbnAsync("ISBN"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                when(obraRepository.findByTituloAndAutor(anyString(), anyString()))
                                .thenReturn(Optional.of(
                                                Obra.builder()
                                                                .titulo("t")
                                                                .autor("a")
                                                                .build()));

                when(livroRepository.save(any(Livro.class)))
                                .thenAnswer(i -> i.getArgument(0));

                assertDoesNotThrow(
                                () -> service.cadastrarVenda("user@email.com", dto, f));

                verify(livroRepository).save(any(Livro.class));
        }

        @Test
        void cadastrarVenda_quandoGoogleBooksRetornaSemItens_deveLancarIllegalArgumentException() {
                LivroRequest dto = new LivroRequest();
                dto.setTitulo("T");
                dto.setAutor("A");
                dto.setIsbn("ISBN-NOTFOUND");

                Cliente vendedor = new Cliente();
                vendedor.setId(1L);
                vendedor.setEmail("user@email.com");

                MultipartFile f = mock(MultipartFile.class);
                when(f.isEmpty()).thenReturn(false);
                when(f.getOriginalFilename()).thenReturn("capa.jpg");
                try {
                        when(f.getInputStream()).thenReturn(
                                        new ByteArrayInputStream(
                                                        new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 }));
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }

                when(clienteRepository.findByEmail("user@email.com")).thenReturn(Optional.of(vendedor));

                GoogleBookData resp = new GoogleBookData();
                resp.setItems(List.of());

                when(googleBooksService.buscarPorIsbnAsync("ISBN-NOTFOUND"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                assertThrows(IllegalArgumentException.class, () -> service.cadastrarVenda("user@email.com", dto, f));
                verify(livroRepository, never()).save(any(Livro.class));
        }

        @Test
        void cadastrarVenda_quandoObraExiste_deveReutilizarObra() {

                LivroRequest dto = new LivroRequest();
                dto.setTitulo("Titulo");
                dto.setAutor("AutorDTO");
                dto.setIsbn("ISBN-EXISTS");

                Cliente vendedor = new Cliente();
                vendedor.setId(1L);
                vendedor.setEmail("user@email.com");

                MultipartFile f = mock(MultipartFile.class);
                when(f.isEmpty()).thenReturn(false);
                when(f.getOriginalFilename()).thenReturn("capa.jpg");
                try {
                        when(f.getInputStream()).thenReturn(
                                        new ByteArrayInputStream(
                                                        new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 }));
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }

                when(clienteRepository.findByEmail("user@email.com")).thenReturn(Optional.of(vendedor));

                GoogleBookData resp = new GoogleBookData();
                GoogleBookData.Item item = new GoogleBookData.Item();
                GoogleBookData.VolumeInfo volumeInfo = new GoogleBookData.VolumeInfo();
                volumeInfo.setTitle("Titulo");
                volumeInfo.setAuthors(List.of("AutorDTO"));
                volumeInfo.setLanguage("pt");
                item.setVolumeInfo(volumeInfo);
                resp.setItems(List.of(item));

                when(googleBooksService.buscarPorIsbnAsync("ISBN-EXISTS"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                Obra obraExistente = Obra.builder().titulo("Titulo").autor("AutorDTO").build();
                when(obraRepository.findByTituloAndAutor("Titulo", "AutorDTO"))
                                .thenReturn(Optional.of(obraExistente));

                when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

                Livro saved = assertDoesNotThrow(() -> service.cadastrarVenda("user@email.com", dto, f));
                assertNotNull(saved);
                assertSame(obraExistente, saved.getObra());

                verify(obraRepository, never()).save(any(Obra.class));
        }

        @Test
        void cadastrarVenda_quandoGoogleBooksSemAutores_deveUsarAutorDesconhecido() throws IOException {
                LivroRequest dto = new LivroRequest();
                dto.setTitulo("Titulo");
                dto.setAutor("AutorDTO");
                dto.setIsbn("ISBN-AUTOR-DESCONHECIDO");

                Cliente vendedor = new Cliente();
                vendedor.setId(1L);
                vendedor.setEmail("user@email.com");

                MultipartFile f = mock(MultipartFile.class);
                when(f.isEmpty()).thenReturn(false);
                when(f.getOriginalFilename()).thenReturn("capa.jpg");
                when(f.getInputStream()).thenReturn(
                                new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 }));

                when(clienteRepository.findByEmail("user@email.com"))
                                .thenReturn(Optional.of(vendedor));

                GoogleBookData resp = new GoogleBookData();
                GoogleBookData.Item item = new GoogleBookData.Item();
                GoogleBookData.VolumeInfo volumeInfo = new GoogleBookData.VolumeInfo();
                volumeInfo.setTitle("Titulo");
                volumeInfo.setAuthors(null); // branch autor desconhecido
                item.setVolumeInfo(volumeInfo);
                resp.setItems(List.of(item));

                when(googleBooksService.buscarPorIsbnAsync("ISBN-AUTOR-DESCONHECIDO"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                // faz cair no cadastro/uso da obra; a consulta usa dto.getTitulo/dto.getAutor
                // do serviço helper
                when(obraRepository.findByTituloAndAutor(anyString(), anyString()))
                                .thenReturn(Optional.empty());
                when(obraRepository.save(any(Obra.class))).thenAnswer(i -> i.getArgument(0));
                when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

                Livro saved = assertDoesNotThrow(() -> service.cadastrarVenda("user@email.com", dto, f));
                assertNotNull(saved);
                assertNotNull(saved.getObra());
                assertNotNull(saved.getObra().getAutor());
                verify(livroRepository).save(any(Livro.class));
        }

        @Test
        void criarLote_quandoLimiteDeLotesPendentesAtingido_deveLancarIllegalStateException() {
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("user@email.com");

                LoteRequest dto = mock(LoteRequest.class);
                when(clienteRepository.findByEmail("user@email.com")).thenReturn(Optional.of(cliente));
                when(loteService.countPendingByCliente(1L)).thenReturn(5L);

                assertThrows(IllegalStateException.class, () -> service.criarLote("user@email.com", dto, List.of()));
                verify(loteRepository, never()).save(any(Lote.class));
        }

        @Test
        void criarLote_quandoPossuiDoisItens_eFotosIncluindoNula_deveSalvarLoteELivros() throws IOException {
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("user@email.com");

                LivroItemRequest item1 = mock(LivroItemRequest.class);
                when(item1.getTitulo()).thenReturn("T1");
                when(item1.getAutor()).thenReturn("A1");
                when(item1.getIsbn()).thenReturn("ISBN1");
                when(item1.getQuantidadedeFotos()).thenReturn(1);

                LivroItemRequest item2 = mock(LivroItemRequest.class);
                when(item2.getTitulo()).thenReturn("T2");
                when(item2.getAutor()).thenReturn("A2");
                when(item2.getIsbn()).thenReturn("ISBN2");
                when(item2.getQuantidadedeFotos()).thenReturn(0);

                // qtdFotos==0 => usa divisão fotos.size()/dto.livros.size()
                LoteRequest dto = mock(LoteRequest.class);
                when(dto.getLivros()).thenReturn(List.of(item1, item2));

                MultipartFile foto1 = mock(MultipartFile.class);
                when(foto1.isEmpty()).thenReturn(false);
                when(foto1.getOriginalFilename()).thenReturn("capa.jpg");
                when(foto1.getInputStream()).thenReturn(
                                new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 }));

                MultipartFile fotoNull = null;
                MultipartFile foto2 = mock(MultipartFile.class);
                lenient().when(foto2.isEmpty()).thenReturn(false);
                lenient().when(foto2.getOriginalFilename()).thenReturn("capa2.jpg");
                lenient().when(foto2.getInputStream()).thenReturn(
                        new ByteArrayInputStream(new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0 }));

                List<MultipartFile> fotos = Arrays.asList(foto1, null, foto2);

                when(clienteRepository.findByEmail("user@email.com")).thenReturn(Optional.of(cliente));
                when(loteService.countPendingByCliente(1L)).thenReturn(0L);
                when(loteRepository.save(any(Lote.class))).thenAnswer(i -> {
                        Lote l = i.getArgument(0);
                        l.setId(10L);
                        return l;
                });
                when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

                Lote lote = service.criarLote("user@email.com", dto, fotos);
                assertNotNull(lote);
                verify(livroRepository, atLeastOnce()).save(any(Livro.class));
                verify(logAuditoria).registrarLog(eq("LOTE_CADASTRO_CRIADO"), eq(1L), eq("user@email.com"),
                                contains("loteId="));
        }

        @Test
        void cadastrarPorIsbn_quandoGoogleBooksTemItens_deveCriarLivroELog() {
                GoogleBookData resp = new GoogleBookData();
                GoogleBookData.Item item = new GoogleBookData.Item();
                GoogleBookData.VolumeInfo volumeInfo = new GoogleBookData.VolumeInfo();
                volumeInfo.setTitle("Titulo");
                volumeInfo.setAuthors(List.of("Autor"));
                volumeInfo.setLanguage("pt");
                volumeInfo.setDescription("Resumo");
                item.setVolumeInfo(volumeInfo);
                resp.setItems(List.of(item));

                when(googleBooksService.buscarPorIsbnAsync("ISBN1"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                Livro livro = service.cadastrarPorIsbn("ISBN1");
                assertNotNull(livro);
                assertEquals("ISBN1", livro.getIsbn());
                verify(logAuditoria).registrarLog(eq("LIVRO_ISBN_PROCESSADO"), isNull(), isNull(),
                                contains("origem=GOOGLE_BOOKS"));
        }

        @Test
        void cadastrarPorIsbn_quandoGoogleBooksSemItens_eFallbackPresente_deveRetornarFallback() {
                GoogleBookData resp = new GoogleBookData();
                resp.setItems(List.of());
                when(googleBooksService.buscarPorIsbnAsync("ISBN2"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                Livro fallback = Livro.builder().isbn("ISBN2").build();
                when(googleBooksService.buscarPorIsbnOpenLibrary("ISBN2")).thenReturn(Optional.of(fallback));

                Livro result = service.cadastrarPorIsbn("ISBN2");
                assertSame(fallback, result);
        }

        @Test
        void cadastrarPorIsbn_quandoGoogleBooksSemItens_eFallbackAusente_deveLancarEntityNotFound() {
                GoogleBookData resp = new GoogleBookData();
                resp.setItems(List.of());
                when(googleBooksService.buscarPorIsbnAsync("ISBN3"))
                                .thenReturn(CompletableFuture.completedFuture(resp));

                when(googleBooksService.buscarPorIsbnOpenLibrary("ISBN3"))
                                .thenReturn(Optional.empty());

                assertThrows(jakarta.persistence.EntityNotFoundException.class,
                                () -> service.cadastrarPorIsbn("ISBN3"));
        }

        @Test
        void listarPromocoesAtivas_quandoExisteLista_deveMapearCamposELogar() {
                Livro l1 = new Livro();
                l1.setId(1L);
                l1.setTitulo("T1");
                l1.setAutor("A1");
                l1.setIsbn("I1");
                l1.setPrecoAprovado(10.0);

                List<Livro> encontrados = List.of(l1);
                when(livroRepository.findPromocoesAtivas(any(LocalDateTime.class))).thenReturn(encontrados);

                List<Livro> result = service.listarPromocoesAtivas();
                assertEquals(1, result.size());
                assertEquals("T1", result.get(0).getTitulo());
                verify(logAuditoria).registrarLog(eq("LIVRO_PROMOCOES_ATIVAS_LISTADAS"), isNull(), isNull(),
                                contains("total=1"));
        }

        @Test
        void buscarPorIdAtivo_quandoEncontrado_deveRetornarLivroELog() {
                Livro l = new Livro();
                l.setId(5L);
                when(livroRepository.findByIdAndAprovadoTrue(5L)).thenReturn(Optional.of(l));

                Livro result = service.buscarPorIdAtivo(5L);
                assertSame(l, result);
                verify(logAuditoria).registrarLog(eq("LIVRO_BUSCA_ATIVO"), isNull(), isNull(), contains("livroId=5"));
        }
}
