package umc.exs.service.core.livros.recompensa;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.gamificacao.GamificacaoService;

@ExtendWith(MockitoExtension.class)
class LivroRecompensaServiceTest {

    @Mock
    GamificacaoService gamificacaoService;

    @InjectMocks
    LivroRecompensaService service;

    @Test
    void recompensarVendedorPorAprovacao_quandoVendedorNull_naoFalha() {
        service.recompensarVendedorPorAprovacao(null);
        verifyNoInteractions(gamificacaoService);
    }

    @Test
    void recompensarVendedorPorAprovacao_quandoFalhaNaoPropaga() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("a@test.com");
        doThrow(new RuntimeException("erro")).when(gamificacaoService).xpLivroAprovado(1L);

        service.recompensarVendedorPorAprovacao(cliente);

        verify(gamificacaoService).xpLivroAprovado(1L);
    }
}
