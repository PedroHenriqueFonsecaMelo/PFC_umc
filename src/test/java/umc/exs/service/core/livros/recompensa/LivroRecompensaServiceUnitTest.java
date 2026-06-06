package umc.exs.service.core.livros.recompensa;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.gamificacao.GamificacaoService;

@ExtendWith(MockitoExtension.class)
class LivroRecompensaServiceUnitTest {

    @Mock
    GamificacaoService gamificacaoService;

    @InjectMocks
    LivroRecompensaService service;

    @Test
    void recompensarVendedorPorAprovacao_quandoVendedorNull_naoDeveChamarGamificacao() {
        service.recompensarVendedorPorAprovacao(null);

        verifyNoInteractions(gamificacaoService);
    }

    @Test
    void recompensarVendedorPorAprovacao_quandoGamificacaoDisparaExcecao_deveEngolir() {

        Cliente c = new Cliente();
        c.setId(1L);
        c.setEmail("x@test.com");

        doThrow(new RuntimeException("erro"))
                .when(gamificacaoService)
                .xpLivroAprovado(1L);

        assertDoesNotThrow(() ->
                service.recompensarVendedorPorAprovacao(c)
        );

        verify(gamificacaoService).xpLivroAprovado(1L);
    }

    @Test
    void recompensarVendedorPorAprovacao_quandoSucesso_deveChamarGamificacao() {

        Cliente c = new Cliente();
        c.setId(2L);
        c.setEmail("y@test.com");

        doNothing()
                .when(gamificacaoService)
                .xpLivroAprovado(2L);

        assertDoesNotThrow(() ->
                service.recompensarVendedorPorAprovacao(c)
        );

        verify(gamificacaoService).xpLivroAprovado(2L);
    }
}