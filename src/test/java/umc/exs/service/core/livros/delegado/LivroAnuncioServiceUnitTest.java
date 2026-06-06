package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
import umc.exs.dto.request.livro.LivroRequest;
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
}