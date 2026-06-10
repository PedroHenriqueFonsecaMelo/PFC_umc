package umc.exs.service.core.interactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.cliente.NovoTopicoRequest;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.AppLogger;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock
    TopicoForumRepository topicoRepo;

    @Mock
    RespostaForumRepository respostaRepo;

    @Mock
    ClienteRepository clienteRepo;

    @Mock
    GamificacaoService gamificacaoService;

    @InjectMocks
    ForumService service;

    @Mock
    AppLogger appLogger;

    @Mock
    LogAuditoriaService logAuditoriaService;

    @Test
    void criarTopico_quandoUsuarioExiste_salvaTopico() {
        Cliente autor = new Cliente();
        autor.setId(1L);
        when(clienteRepo.findById(1L)).thenReturn(Optional.of(autor));
        when(topicoRepo.save(any(TopicoForum.class))).thenAnswer(i -> i.getArgument(0));

        NovoTopicoRequest dto = new NovoTopicoRequest();
        dto.setTitulo("Título");
        dto.setConteudo("Conteúdo");
        dto.setCategoria(CategoriaForum.GERAL);

        TopicoForum topico = service.criarTopico(dto, 1L);

        assertEquals("Título", topico.getTitulo());
        assertSame(autor, topico.getAutor());
    }

    @Test
    void isAutorResposta_quandoEmailCorresponde_retornaTrue() {
        RespostaForum resposta = new RespostaForum();
        resposta.setId(5L);
        Cliente autor = new Cliente();
        autor.setEmail("user@test.com");
        resposta.setAutor(autor);
        when(respostaRepo.findById(5L)).thenReturn(Optional.of(resposta));

        assertTrue(service.isAutorResposta(5L, "user@test.com"));
    }

    @Test
    void listarTopicos_semFiltros_chamaFindAll() {
        Pageable pageable = mock(Pageable.class);
        when(topicoRepo.findAll(pageable)).thenReturn(Page.empty());

        service.listarTopicos(null, null, pageable);

        verify(topicoRepo).findAll(pageable);
    }

    @Test
    void listarTopicos_comBuscaEcategoria() {
        Pageable pageable = mock(Pageable.class);

        service.listarTopicos("java", CategoriaForum.GERAL, pageable);

        verify(topicoRepo)
                .findByTituloContainingIgnoreCaseAndCategoria("java", CategoriaForum.GERAL, pageable);
    }

    @Test
    void buscarTopicoPorId_quandoExiste_retornaTopico() {
        TopicoForum topico = new TopicoForum();
        topico.setId(1L);

        when(topicoRepo.findByIdWithRespostas(1L)).thenReturn(Optional.of(topico));

        TopicoForum resultado = service.buscarTopicoPorId(1L);

        assertEquals(1L, resultado.getId());
    }

    @Test
    void buscarTopicoPorId_quandoNaoExiste_lancaErro() {
        when(topicoRepo.findByIdWithRespostas(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            service.buscarTopicoPorId(1L);
        });
    }

    @Test
    void incrementarVisualizacoes_deveChamarRepository() {
        service.incrementarVisualizacoes(1L);

        verify(topicoRepo).incrementarVisualizacoes(1L);
    }

    @Test
    void getRespostasLikedByUser_quandoClienteNull_retornaVazio() {
        var resultado = service.getRespostasLikedByUser(1L, null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void criarResposta_deveSalvarRespostaEIncrementarTopico() {
        TopicoForum topico = new TopicoForum();
        topico.setId(1L);
        topico.setQtdRespostas(0);

        Cliente autor = new Cliente();
        autor.setId(2L);

        when(topicoRepo.findById(1L)).thenReturn(Optional.of(topico));
        when(clienteRepo.findById(2L)).thenReturn(Optional.of(autor));
        when(respostaRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        RespostaForum resposta = service.criarResposta(1L, "Conteudo", 2L);

        assertEquals(1, topico.getQtdRespostas());
        verify(topicoRepo).save(topico);
        assertNotNull(resposta);
    }

    @Test
    void deletarTopico_deveChamarDelete() {
        service.deletarTopico(1L);

        verify(topicoRepo).deleteById(1L);
    }

    @Test
    void isAutorResposta_quandoNaoExiste_retornaFalse() {
        when(respostaRepo.findById(1L)).thenReturn(Optional.empty());

        assertFalse(service.isAutorResposta(1L, "email@test.com"));
    }

    @Test
    void deletarResposta_deveRemoverEReduzirContador() {
        TopicoForum topico = new TopicoForum();
        topico.setQtdRespostas(2);

        Cliente autor = new Cliente();
        autor.setId(1L);
        autor.setEmail("email@test.com");

        RespostaForum resposta = new RespostaForum();
        resposta.setId(1L);
        resposta.setTopico(topico);
        resposta.setAutor(autor);

        when(respostaRepo.findById(1L)).thenReturn(Optional.of(resposta));

        service.deletarResposta(1L);

        assertEquals(1, topico.getQtdRespostas());
        verify(respostaRepo).delete(resposta);
        verify(topicoRepo).save(topico);
    }

    @Test
    void curtirResposta_quandoNaoCurtiu_adicionaLike() {
        RespostaForum resposta = new RespostaForum();
        resposta.setId(1L);
        resposta.setQtdCurtidas(0);
        resposta.setCurtidoresIds(new java.util.HashSet<>());

        when(respostaRepo.findById(1L)).thenReturn(Optional.of(resposta));
        when(respostaRepo.save(any())).thenReturn(resposta);

        var resultado = service.curtirResposta(1L, 10L);

        assertEquals(1, resultado.get("curtidas"));
        assertEquals(true, resultado.get("liked"));
    }

    @Test
    void curtirResposta_quandoJaCurtiu_removeLike() {
        RespostaForum resposta = new RespostaForum();
        resposta.setId(1L);
        resposta.setQtdCurtidas(1);
        resposta.setCurtidoresIds(new java.util.HashSet<>(java.util.Set.of(10L)));

        when(respostaRepo.findById(1L)).thenReturn(Optional.of(resposta));
        when(respostaRepo.save(any())).thenReturn(resposta);

        var resultado = service.curtirResposta(1L, 10L);

        assertEquals(0, resultado.get("curtidas"));
        assertEquals(false, resultado.get("liked"));
    }

    @Test
    void marcarMelhorResposta_quandoSemPermissao_lancaErro() {
        TopicoForum topico = new TopicoForum();
        Cliente autorTopico = new Cliente();
        autorTopico.setId(1L);
        topico.setAutor(autorTopico);

        RespostaForum resposta = new RespostaForum();
        resposta.setId(5L);
        resposta.setTopico(topico);

        when(respostaRepo.findById(5L)).thenReturn(Optional.of(resposta));

        assertThrows(RuntimeException.class, () -> {
            service.marcarMelhorResposta(5L, 2L, false);
        });
    }

    @Test
    void marcarMelhorResposta_quandoPermitido_defineComoMelhor() {
        TopicoForum topico = new TopicoForum();
        Cliente autorTopico = new Cliente();
        autorTopico.setId(1L);
        topico.setAutor(autorTopico);

        Cliente autorResposta = new Cliente();
        autorResposta.setId(2L);

        RespostaForum resposta = new RespostaForum();
        resposta.setId(5L);
        resposta.setTopico(topico);
        resposta.setAutor(autorResposta);
        resposta.setCurtidoresIds(new java.util.HashSet<>());

        when(respostaRepo.findById(5L)).thenReturn(Optional.of(resposta));
        when(respostaRepo.findByTopicoAndMelhorRespostaTrue(topico))
                .thenReturn(Optional.empty());

        service.marcarMelhorResposta(5L, 1L, false);

        assertTrue(resposta.isMelhorResposta());
        assertTrue(topico.isResolvido());

        verify(respostaRepo).save(resposta);
        verify(topicoRepo).save(topico);
    }
}
