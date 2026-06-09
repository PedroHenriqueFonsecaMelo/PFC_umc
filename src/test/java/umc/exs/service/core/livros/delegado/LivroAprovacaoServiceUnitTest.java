package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;

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

    @Test
    void aprovarLivro_quandoGeneroEmBranco_deveBuscarGeneroNaAPI() {
        Livro anuncio = new Livro();
        anuncio.setId(1L);
        anuncio.setIsbn("ISBN");
        anuncio.setTitulo("Titulo");
        anuncio.setGenero(" "); // ativa branch (blank)
        anuncio.setPrecoAprovado(null);

        Cliente vendedor = new Cliente();
        vendedor.setId(10L);
        vendedor.setEmail("vendedor@email.com");
        vendedor.setNome("Vendedor");
        vendedor.setSaldoTokens(0.0);

        anuncio.setVendedor(vendedor);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

        // sem lote pra evitar branch de status do lote (foco no genero)
        anuncio.setLote(null);

        when(googleBooksService.buscarGeneroPorIsbn("ISBN")).thenReturn("GENERO");

        Livro saved = service.aprovarLivro(1L, 2L, dto);

        assertEquals("GENERO", saved.getGenero());
        verify(googleBooksService).buscarGeneroPorIsbn("ISBN");
    }

    @Test
    void rejeitarLivro_quandoLoteExiste_ePendentesZero_deveAtualizarStatusTotal() {
        Livro anuncio = new Livro();
        anuncio.setId(1L);
        anuncio.setIsbn("ISBN");
        anuncio.setTitulo("Titulo");
        anuncio.setMotivoRejeicao(null);

        Cliente vendedor = new Cliente();
        vendedor.setEmail("v@e.com");
        vendedor.setNome("V");
        anuncio.setVendedor(vendedor);

        Lote lote = new Lote();
        lote.setId(100L);
        anuncio.setLote(lote);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(anuncio));

        when(livroRepository.countByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(100L))
                .thenReturn(0L);

        when(livroRepository.findByLoteId(100L)).thenReturn(List.of(anuncio));

        // filtro .filter(l -> Boolean.TRUE.equals(l.getAprovado())) => count 0 =>
        // REJEITADO
        anuncio.setAprovado(Boolean.FALSE);

        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));
        when(loteRepository.save(any(Lote.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> service.rejeitarLivro(1L, 2L, "REJEITADO", "motivo"));

        verify(loteRepository).save(any(Lote.class));
        verify(logAuditoria).registrarLog(eq("LIVRO_REJEITADO"), eq(2L), isNull(), contains("motivo"));
    }

    @Test
    void aprovarLivro_quandoGeneroAPIFalha_deveSeguirSemEstourar() {
        Livro anuncio = new Livro();
        anuncio.setId(1L);
        anuncio.setIsbn("ISBN");
        anuncio.setTitulo("Titulo");
        anuncio.setGenero("");

        Cliente vendedor = new Cliente();
        vendedor.setId(10L);
        vendedor.setEmail("vendedor@email.com");
        vendedor.setNome("Vendedor");
        vendedor.setSaldoTokens(0.0);
        anuncio.setVendedor(vendedor);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(livroRepository.countByLoteIdAndAprovadoFalse(anyLong())).thenReturn(1L);

        lenient().when(googleBooksService.buscarGeneroPorIsbn("ISBN")).thenThrow(new RuntimeException("falha"));

        Livro saved = assertDoesNotThrow(() -> service.aprovarLivro(1L, 2L, dto));
        assertNotNull(saved);
    }

    @Test
    void aprovarLivro_quandoVendedorNulo_deveUsarClienteDoLote() {
        Livro anuncio = new Livro();
        anuncio.setId(1L);
        anuncio.setIsbn("ISBN");
        anuncio.setTitulo("Titulo");
        anuncio.setGenero(null);

        Lote lote = new Lote();
        lote.setId(100L);
        Cliente loteCliente = new Cliente();
        loteCliente.setEmail("lote@email.com");
        loteCliente.setNome("LoteVendedor");
        loteCliente.setSaldoTokens(0.0);
        lote.setCliente(loteCliente);
        anuncio.setVendedor(null);
        anuncio.setLote(lote);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));
        when(livroRepository.countByLoteIdAndAprovadoFalse(100L)).thenReturn(0L);
        when(loteRepository.findById(100L)).thenReturn(Optional.of(lote));
        when(loteRepository.save(any(Lote.class))).thenAnswer(i -> i.getArgument(0));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Livro saved = assertDoesNotThrow(() -> service.aprovarLivro(1L, 2L, dto));
        assertNotNull(saved);
        verify(clienteRepository).save(loteCliente);
        verify(livroNotificacaoService).notificarAprovacaoDashboard(eq(loteCliente), eq("Titulo"), anyDouble());
    }

    @Test
    void rejeitarLivro_quandoPendentesNaoZero_naoDeveAtualizarLoteStatus() {
        Livro anuncio = new Livro();
        anuncio.setId(1L);
        anuncio.setIsbn("ISBN");
        anuncio.setTitulo("Titulo");
        anuncio.setMotivoRejeicao(null);

        Cliente vendedor = new Cliente();
        vendedor.setEmail("v@e.com");
        vendedor.setNome("V");
        anuncio.setVendedor(vendedor);

        Lote lote = new Lote();
        lote.setId(100L);
        anuncio.setLote(lote);

        when(livroRepository.findById(1L)).thenReturn(Optional.of(anuncio));
        when(livroRepository.countByLoteIdAndAprovadoFalseAndAdminAprovadorIdIsNull(100L)).thenReturn(3L);
        when(livroRepository.save(any(Livro.class))).thenAnswer(i -> i.getArgument(0));

        service.rejeitarLivro(1L, 2L, "REJEITADO", "motivo");

        verify(loteRepository, never()).save(any(Lote.class));
        verify(logAuditoria).registrarLog(eq("LIVRO_REJEITADO"), eq(2L), isNull(), contains("motivo"));
    }
}
