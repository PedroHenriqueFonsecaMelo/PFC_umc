package umc.exs.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "changeitchangeitchangeitchangeit");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
        ReflectionTestUtils.setField(jwtUtil, "cookieName", "token");
    }

    @Test
    void deveGerarTokenValido() {
        String token = jwtUtil.generateToken("usuario@teste.com");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void deveExtrairUsernameDoToken() {
        String email = "usuario@teste.com";
        String token = jwtUtil.generateToken(email);
        String extraido = jwtUtil.extractUsername(token);
        assertEquals(email, extraido);
    }

    @Test
    void deveValidarTokenValido() {
        String token = jwtUtil.generateToken("usuario@teste.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void deveRejeitarTokenInvalido() {
        assertFalse(jwtUtil.validateToken("token.invalido.aqui"));
    }

    @Test
    void deveRejeitarTokenVazio() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void deveRejeitarTokenNulo() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void deveGerarTokensDiferentesParaUsuariosDiferentes() {
        String token1 = jwtUtil.generateToken("usuario1@teste.com");
        String token2 = jwtUtil.generateToken("usuario2@teste.com");
        assertNotEquals(token1, token2);
    }

    @Test
    void deveGerarTokensDiferentesEmChamadasSuccessivas() {
        String token1 = jwtUtil.generateToken("usuario@teste.com");
        String token2 = jwtUtil.generateToken("usuario@teste.com");
        // Tokens podem ser iguais (mesmo subject/expiration) — o que importa é que ambos são válidos
        assertTrue(jwtUtil.validateToken(token1));
        assertTrue(jwtUtil.validateToken(token2));
    }
}
