package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.admin.AdminAprovacaoRequest;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.api.ExternApi;
import umc.exs.service.core.livros.notificacao.LivroNotificacaoService;
import umc.exs.service.core.livros.recompensa.LivroRecompensaService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class LivroAprovacaoServiceUnitTest {

    @Mock
    LivroRepository livroRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    LoteRepository loteRepository;

    @Mock
    LogAuditoriaService logAuditoria;

    @Mock
    EmailFacade emailFacade;

    @Mock
    ExternApi googleBooksService;

    @Mock
    LivroNotificacaoService livroNotificacaoService;

    @Mock
    LivroRecompensaService livroRecompensaService;

    @InjectMocks
    LivroAprovacaoService service;

    private AdminAprovacaoRequest dto;

    @BeforeEach
    void setUp() {
        dto = new AdminAprovacaoRequest();
        dto.setEstadoAprovado(EstadoLivro.BOM);
        dto.setFotosUrls(null);
    }

    @Test
    void aprovarLivro_quandoLivroExiste_deveAprovarESalvar() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setIsbn("ISBN");
        livro.setTitulo("Titulo");
        livro.setGenero("");
        livro.setPrecoAprovado(null);

        Cliente vendedor = new Cliente();
        vendedor.setId(10L);
        vendedor.setEmail("vendedor@email.com");
        vendedor.setNome("Vendedor");
        vendedor.setSaldoTokens(0.0);

        livro.setVendedor(vendedor);

        Lote lote = new Lote();
        lote.setId(100L);
        livro.setLote(lote);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(livro));
        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

        when(livroRepository.countByLoteIdAndAprovadoFalse(100L)).thenReturn(0L);
        when(loteRepository.findById(100L)).thenReturn(Optional.of(lote));
        when(loteRepository.save(any(Lote.class))).thenAnswer(i -> i.getArgument(0));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Livro saved = service.aprovarLivro(1L, 2L, dto);

        assertEquals(true, saved.getAprovado());
        assertNotNull(saved.getEstadoAprovado());
        verify(livroRepository).save(livro);
        verify(clienteRepository).save(vendedor);
        verify(livroNotificacaoService).notificarAprovacaoDashboard(eq(vendedor), eq(saved.getTitulo()), anyDouble());
        verify(livroNotificacaoService).notificarWishlistSeDisponivel(eq("ISBN"), eq("Titulo"));
    }

    @Test
    void aprovarLivro_quandoLivroNaoEncontrado_deveLancarRuntimeException() {
        when(livroRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.aprovarLivro(999L, 1L, dto));
    }
}
