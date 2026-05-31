package umc.exs.service.cliente.senha;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class SenhaServiceTest {

    @Mock
    RecuperacaoSenhaRepository recuperacaoSenhaRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    EmailFacade emailFacade;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    LogAuditoriaService logAuditoriaService;

    @InjectMocks
    SenhaService service;

    @Test
    void iniciarRecuperacao_deveSalvarTokenEEEnviarEmail() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("c@test.com");
        cliente.setNome("Cliente");

        lenient().when(passwordEncoder.encode(anyString())).thenReturn("hash");

        service.iniciarRecuperacao(cliente);

        verify(recuperacaoSenhaRepository).deleteByCliente(cliente);
        verify(recuperacaoSenhaRepository).save(any(RecuperacaoSenha.class));
        verify(emailFacade).sendHtmlSafe(eq(cliente.getEmail()), anyString(), anyString());
    }

    @Test
    void iniciarRecuperacao_quandoEmailFacadeFalha_deveLancarIllegalStateException() {
        Cliente cliente = new Cliente();
        cliente.setEmail("c@test.com");
        cliente.setNome("Cliente");

        doThrow(new RuntimeException("smtp"))
                .when(emailFacade).sendHtmlSafe(anyString(), anyString(), anyString());

        assertThrows(IllegalStateException.class, () -> service.iniciarRecuperacao(cliente));
    }

    @Test
    void alterarSenhaComToken_deveAtualizarSenhaERemoverToken() {
        String token = UUID.randomUUID().toString();
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("c@test.com");
        cliente.setNome("Cliente");
        cliente.setSenha("old");

        RecuperacaoSenha rec = new RecuperacaoSenha();
        rec.setToken(token);
        rec.setCliente(cliente);
        rec.setDataExpiracao(LocalDateTime.now().plusMinutes(10));

        when(recuperacaoSenhaRepository.findByToken(token)).thenReturn(Optional.of(rec));
        when(passwordEncoder.encode("novaSenha!1@"))
                .thenReturn("newHash");

        service.alterarSenhaComToken(token, "novaSenha!1@");

        assertEquals("newHash", cliente.getSenha());
        verify(clienteRepository).save(cliente);
        verify(logAuditoriaService).registrarLog(eq("SENHA_ALTERADA"), eq(cliente.getId()), eq(cliente.getEmail()),
                anyString());
        verify(recuperacaoSenhaRepository).delete(rec);
    }

    @Test
    void alterarSenhaComToken_tokenExpirado_deveLancarIllegalArgumentException() {
        String token = UUID.randomUUID().toString();

        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("c@test.com");
        cliente.setNome("Cliente");

        RecuperacaoSenha rec = new RecuperacaoSenha();
        rec.setToken(token);
        rec.setCliente(cliente);
        rec.setDataExpiracao(LocalDateTime.now().minus(1, ChronoUnit.MINUTES));

        when(recuperacaoSenhaRepository.findByToken(token)).thenReturn(Optional.of(rec));

        assertThrows(IllegalArgumentException.class,
                () -> service.alterarSenhaComToken(token, "novaSenha!1@"));
        verify(recuperacaoSenhaRepository, never()).delete(any());
    }

    @Test
    void isTokenValido_trueQuandoNaoExpirado() {
        String token = "t";
        RecuperacaoSenha rec = new RecuperacaoSenha();
        rec.setToken(token);
        rec.setDataExpiracao(LocalDateTime.now().plusMinutes(5));

        when(recuperacaoSenhaRepository.findByToken(token)).thenReturn(Optional.of(rec));

        assertTrue(service.isTokenValido(token));
    }

    @Test
    void isTokenValido_falseQuandoNaoExiste() {
        when(recuperacaoSenhaRepository.findByToken("x")).thenReturn(Optional.empty());
        assertFalse(service.isTokenValido("x"));
    }
}
