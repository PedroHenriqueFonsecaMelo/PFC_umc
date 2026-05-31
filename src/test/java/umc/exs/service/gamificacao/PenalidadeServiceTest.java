package umc.exs.service.gamificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PenalidadeServiceTest {

    @Mock
    PontuacaoUsuarioRepository pontuacaoRepository;

    @InjectMocks
    PenalidadeService service;

    @Test
    void aplicarPenalidadeTodos_quandoXpNegativo_naoAltera() {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setXpTotal(0);
        p.setUltimaAtualizacao(LocalDateTime.now().minusDays(100));

        when(pontuacaoRepository.findAll()).thenReturn(List.of(p));
        when(pontuacaoRepository.saveAll(any())).thenReturn(List.of(p));

        service.aplicarPenalidadeTodos();

        assertEquals(0, p.getXpTotal());
        verify(pontuacaoRepository).saveAll(List.of(p));
    }

    @Test
    void aplicarPenalidadeTodos_quandoXpApos30Dias_reduzOuZera() {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setXpTotal(100);
        p.setXpLivrosAprovados(20);
        p.setXpCompras(20);
        p.setXpAvaliacoes(20);
        p.setUltimaAtualizacao(LocalDateTime.now().minusDays(40));

        when(pontuacaoRepository.findAll()).thenReturn(List.of(p));
        when(pontuacaoRepository.saveAll(any())).thenReturn(List.of(p));

        service.aplicarPenalidadeTodos();

        assertTrue(p.getXpTotal() <= 100);
        verify(pontuacaoRepository).saveAll(List.of(p));
    }
}
