package umc.exs.service.core.interactions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

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

@ExtendWith(MockitoExtension.class)
class ForumServiceTest {

    @Mock
    TopicoForumRepository topicoRepo;

    @Mock
    RespostaForumRepository respostaRepo;

    @Mock
    ClienteRepository clienteRepo;

    @InjectMocks
    ForumService service;

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
}
