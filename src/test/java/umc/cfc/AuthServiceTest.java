package umc.cfc;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import umc.cfc.model.User;
import umc.cfc.repository.UserRepository;
import umc.cfc.service.AuthService;

@SpringBootTest
public class AuthServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);
    private static final String USERNAME = "testeuser";
    private static final String CORRECT_PASSWORD = "senha123";
    private static final String WRONG_PASSWORD = "senhaincorreta";
    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        logger.info("SETUP: Assegurando que o usuário '{}' esteja limpo para o teste.", USERNAME);
        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
        authService.register(USERNAME, CORRECT_PASSWORD);
    }

    @AfterEach
    public void cleanup() {
        logger.info("CLEANUP: Removendo o usuário '{}' após o teste.", USERNAME);
        userRepository.findByUsername(USERNAME).ifPresent(userRepository::delete);
    }

    @Test
    public void testRegistroDeUsuarioExistenteFalha() {
        logger.info("Iniciando teste: Tentativa de registro de usuário existente.");
        boolean registroFalha = authService.register(USERNAME, CORRECT_PASSWORD);
        assertFalse(registroFalha, "O registro de um usuário já existente deveria falhar.");
    }

    @Test
    public void testLoginBemSucedidoResetaTentativas() {
        logger.info("Iniciando teste: Login bem-sucedido reseta o contador de tentativas.");

        // Simula 2 tentativas falhas
        authService.login(USERNAME, WRONG_PASSWORD);
        authService.login(USERNAME, WRONG_PASSWORD);

        // Login com a senha correta
        String resultadoLogin = authService.login(USERNAME, CORRECT_PASSWORD);
        assertEquals("usuário logado com sucesso.", resultadoLogin);

        // Verifica se o contador foi resetado
        User user = userRepository.findByUsername(USERNAME).get();
        assertEquals(0, user.getFailedAttempts(), "As tentativas falhas deveriam ter sido resetadas.");
        assertFalse(user.isAccountLocked(), "A conta não deveria estar bloqueada.");
    }

    @Test
    public void testLoginComSenhaIncorretaIncrementaContador() {
        logger.info("Iniciando teste: Login com senha incorreta incrementa o contador.");

        // Simula uma tentativa falha
        authService.login(USERNAME, WRONG_PASSWORD);

        // Verifica se o contador de tentativas falhas aumentou
        User user = userRepository.findByUsername(USERNAME).get();
        assertEquals(1, user.getFailedAttempts(), "O contador de tentativas falhas deveria ser 1.");
        assertFalse(user.isAccountLocked(), "A conta não deveria estar bloqueada.");
    }

    @Test
    public void testBloqueioDeContaAposTentativasFalhas() {
        logger.info("Iniciando teste: Bloqueio de conta após 5 tentativas falhas.");

        // Simula 5 tentativas de login falhas
        for (int i = 0; i < MAX_FAILED_ATTEMPTS; i++) {
            authService.login(USERNAME, WRONG_PASSWORD);
        }

        // A 6ª tentativa deve bloquear a conta
        String resultadoFinal = authService.login(USERNAME, WRONG_PASSWORD);
        assertTrue(resultadoFinal.contains("Muitas tentativas"), "A 6ª tentativa deveria resultar em bloqueio.");

        // Verifica o estado da conta no banco
        User userBloqueado = userRepository.findByUsername(USERNAME).get();
        assertTrue(userBloqueado.isAccountLocked(), "A conta deveria estar bloqueada.");
    }

    @Test
    public void testLoginEmContaBloqueadaFalha() {
        logger.info("Iniciando teste: Login falha em conta bloqueada.");

        // Força o bloqueio da conta
        for (int i = 0; i < MAX_FAILED_ATTEMPTS + 1; i++) {
            authService.login(USERNAME, WRONG_PASSWORD);
        }

        // Tenta fazer login com a senha correta
        String resultadoLogin = authService.login(USERNAME, CORRECT_PASSWORD);
        assertEquals("Usuário bloqueado.", resultadoLogin, "O login deveria falhar pois a conta está bloqueada.");
    }

    @Test
    public void testLoginComUsuarioInexistente() {
        logger.info("Iniciando teste: Login com usuário inexistente.");
        String resultado = authService.login("usuarioInexistente", "qualquerSenha");
        assertEquals("Não há usuário com esse nome.", resultado);
    }
}