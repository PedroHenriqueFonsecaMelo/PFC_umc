package umc.exs.service.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.notificacao.NotificacaoService;

@ExtendWith(MockitoExtension.class)
class PontosSchedulerServiceTest {

    @Mock
    PontuacaoUsuarioRepository pontuacaoRepository;

    @Mock
    LogAuditoriaService logAuditoria;

    @Mock
    NotificacaoService notificacaoService;

    @InjectMocks
    PontosSchedulerService service;

    @Test
    void avisarXpAExpirar_quandoXPPositivo_chamaNotificacao() {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(new umc.exs.model.entidades.usuario.Cliente());
        p.getCliente().setEmail("a@test.com");
        p.setXpTotal(100);
        p.setUltimaAtualizacao(LocalDateTime.now().minusDays(26));

        when(pontuacaoRepository.findAllByUltimaAtualizacaoBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(p));

        service.avisarXpAExpirar();

        verify(notificacaoService).criarNotificacaoDashboard(any(), anyString(), anyString());
    }

    @Test
    void processarDecayXp_quandoInativo_aplicaReducao() {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(new umc.exs.model.entidades.usuario.Cliente());
        p.getCliente().setEmail("a@test.com");
        p.setXpTotal(100);
        p.setXpLivrosAprovados(10);
        p.setXpCompras(10);
        p.setXpAvaliacoes(10);
        p.setUltimaAtualizacao(LocalDateTime.now().minusDays(32));

        when(pontuacaoRepository.findAllByUltimaAtualizacaoBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(p));
        when(pontuacaoRepository.saveAll(any())).thenReturn(List.of(p));

        service.processarDecayXp();

        assertTrue(p.getXpTotal() < 100);
        verify(pontuacaoRepository).saveAll(List.of(p));
    }
}
