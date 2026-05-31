package umc.exs.service.email.notificacao;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import umc.exs.dto.request.admin.EmailDisparoRequest;
import umc.exs.dto.response.email.EmailDestinatarioResponse;
import umc.exs.model.entidades.foundation.EmailEnviado;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.EmailEnviadoRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.email.facade.EmailFacade;

@ExtendWith(MockitoExtension.class)
class NotificacaoEmailServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    PontuacaoUsuarioRepository pontuacaoRepository;

    @Mock
    EmailFacade emailFacade;

    @Mock
    TaskScheduler taskScheduler;

    @Mock
    EmailEnviadoRepository emailEnviadoRepository;

    @InjectMocks
    NotificacaoEmailService service;

    @Test
    void filtrarDestinatarios_quandoFiltroPorTokens_retornaOrdenado() {
        Cliente c1 = new Cliente();
        c1.setId(1L);
        c1.setNome("A");
        c1.setEmail("a@test.com");
        c1.setSaldoTokens(10.0);
        Cliente c2 = new Cliente();
        c2.setId(2L);
        c2.setNome("B");
        c2.setEmail("b@test.com");
        c2.setSaldoTokens(5.0);

        PontuacaoUsuario p1 = new PontuacaoUsuario();
        p1.setCliente(c1);
        p1.setXpTotal(100);
        PontuacaoUsuario p2 = new PontuacaoUsuario();
        p2.setCliente(c2);
        p2.setXpTotal(50);

        when(clienteRepository.findAll()).thenReturn(List.of(c1, c2));
        when(pontuacaoRepository.findAllWithCliente()).thenReturn(List.of(p2, p1));

        List<EmailDestinatarioResponse> result = service.filtrarDestinatarios("tokens_maior", 2);

        assertEquals(2, result.size());
        assertEquals("a@test.com", result.get(0).getEmail());
    }

    @Test
    void dispararOuAgendar_quandoDestinatariosExistem_agendaDisparoImediato() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("A");
        cliente.setEmail("a@test.com");
        PontuacaoUsuario pontuacao = new PontuacaoUsuario();
        pontuacao.setCliente(cliente);
        pontuacao.setXpTotal(0);

        when(clienteRepository.findAll()).thenReturn(List.of(cliente));
        when(pontuacaoRepository.findAllWithCliente()).thenReturn(List.of(pontuacao));
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(null);
        when(emailEnviadoRepository.save(any(EmailEnviado.class))).thenAnswer(i -> i.getArgument(0));

        EmailDisparoRequest dto = new EmailDisparoRequest();
        dto.setFiltro(null);
        dto.setLimite(10);
        dto.setAssunto("Olá");
        dto.setCorpo("Corpo teste");
        dto.setAgendamento(null);

        String resultado = service.dispararOuAgendar(dto);

        assertTrue(resultado.contains("Disparo iniciado"));
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
        verify(emailEnviadoRepository).save(any(EmailEnviado.class));
    }

    @Test
    void dispararOuAgendar_quandoNaoHaDestinatarios_retornaMensagem() {
        when(clienteRepository.findAll()).thenReturn(List.of());
        when(pontuacaoRepository.findAllWithCliente()).thenReturn(List.of());

        EmailDisparoRequest dto = new EmailDisparoRequest();
        dto.setFiltro(null);
        dto.setLimite(10);
        dto.setAssunto("Olá");
        dto.setCorpo("Corpo teste");
        dto.setAgendamento(null);

        String resultado = service.dispararOuAgendar(dto);

        assertTrue(resultado.contains("Nenhum destinatário encontrado"));
        verifyNoInteractions(taskScheduler);
    }
}
