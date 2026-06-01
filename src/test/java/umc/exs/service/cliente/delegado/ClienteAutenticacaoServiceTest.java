package umc.exs.service.cliente.delegado;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.cliente.senha.SenhaService;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class ClienteAutenticacaoServiceTest {

    @Mock
    ClienteRepositoryService repositoryService;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    RecuperacaoSenhaRepository tokenRepository;

    @Mock
    SenhaService senhaService;

    @InjectMocks
    ClienteAutenticacaoService service;

    @Mock
    LogAuditoriaService logAuditoria;

    @Test
    void autenticar_quandoSenhaCorreta_retornaCliente() {
        Cliente cliente = new Cliente();
        cliente.setEmailVerificado(true);
        cliente.setBloqueada(false);
        cliente.setSenha("hash");

        when(repositoryService.encontrarPorEmail("user@test.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("senha", "hash")).thenReturn(true);

        Cliente result = service.autenticar("user@test.com", "senha");

        assertSame(cliente, result);
        verify(repositoryService).resetarTentativasLogin(cliente);
    }

    @Test
    void autenticar_quandoSenhaErrada_registraFalhaELanca() {
        Cliente cliente = new Cliente();
        cliente.setEmailVerificado(true);
        cliente.setBloqueada(false);
        cliente.setSenha("hash");
        cliente.setTentativas(2);

        when(repositoryService.encontrarPorEmail("user@test.com")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.matches("senha", "hash")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.autenticar("user@test.com", "senha"));

        assertTrue(exception.getMessage().contains("Senha incorreta"));
        verify(repositoryService).registrarFalhaLogin(cliente);
    }

    @Test
    void validarToken_quandoTokenValido_retornaTrue() {
        RecuperacaoSenha registro = new RecuperacaoSenha();
        registro.setDataExpiracao(LocalDateTime.now().plusDays(1));
        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(registro));

        assertTrue(service.validarToken("token123"));
    }

    @Test
    void validarToken_quandoTokenInvalido_retornaFalse() {
        when(tokenRepository.findByToken("token123")).thenReturn(Optional.empty());
        assertFalse(service.validarToken("token123"));
    }

    @Test
    void redefinirSenha_quandoTokenValido_atualizaSenhaEDeletaToken() {
        Cliente cliente = new Cliente();
        cliente.setSenha("old");
        RecuperacaoSenha registro = new RecuperacaoSenha();
        registro.setCliente(cliente);
        registro.setDataExpiracao(LocalDateTime.now().plusDays(1));

        when(tokenRepository.findByToken("token123")).thenReturn(Optional.of(registro));
        when(passwordEncoder.encode("nova")).thenReturn("novaHash");

        Cliente result = service.redefinirSenha("token123", "nova");

        assertSame(cliente, result);
        assertEquals("novaHash", cliente.getSenha());
        verify(repositoryService).salvar(cliente);
        verify(tokenRepository).delete(registro);
    }
}
