package umc.exs.service.scheduler;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class CupomSchedulerServiceTest {

    @Mock
    CupomRepository cupomRepository;

    @Mock
    EmailFacade emailFacade;

    @Mock
    NotificacaoService notificacaoService;

    @InjectMocks
    CupomSchedulerService service;

    @Test
    void marcarCuponsExpirados_deveMarcarComoUsado() {
        Cupom c = Cupom.builder().id(1L).usado(false).expiracao(LocalDateTime.now().minusDays(1)).build();
        when(cupomRepository.findByUsadoFalseAndExpiracaoBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(c));
        when(cupomRepository.saveAll(any())).thenReturn(List.of(c));

        service.marcarCuponsExpirados();

        assertTrue(c.isUsado());
        verify(cupomRepository).saveAll(List.of(c));
    }

    @Test
    void avisarCuponsAVencer_quandoTemCupomEnviaEmailENotificacao() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("a@test.com");
        cliente.setNome("Cliente");
        Cupom cupom = Cupom.builder()
                .id(1L)
                .codigo("CUPOM")
                .percentualDesconto(10.0)
                .expiracao(LocalDateTime.now().plusDays(7))
                .usado(false)
                .cliente(cliente)
                .build();

        when(cupomRepository.findByUsadoFalseAndExpiracaoBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cupom));

        service.avisarCuponsAVencer();

        verify(emailFacade).sendHtmlSafe(eq("a@test.com"), anyString(), anyString());
        verify(notificacaoService).criarNotificacaoDashboard(eq(cliente), anyString(), anyString());
    }
}
