package umc.exs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.JwtException;

@Component
public class JwtUtil {

    @Value("${jwt.secret:changeitchangeitchangeitchangeit}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    @Value("${jwt.cookie.name:token}")
    private String cookieName;

    // Helper para gerar a chave segura a partir da String
    /**
     * Gera chave assinatura HMAC-SHA secreta JWT.
     * 
     * @return SecretKey signing
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Gera token JWT com subject, expiração configurada.
     * 
     * @param subject username/email
     * @return token string
     */
    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrai username do payload JWT verificado.
     * 
     * @param token JWT
     * @return subject username
     */
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valida token JWT assinatura/expiração.
     * 
     * @param token JWT
     * @return válido
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Adiciona cookie HTTP-only JWT response.
     * Secure false, path /.
     * 
     * @param response servlet
     * @param token    JWT
     */
    public void addTokenCookie(HttpServletResponse response, String token) {
        Cookie c = new Cookie(cookieName, token);
        c.setHttpOnly(true);
        c.setSecure(true); // obrigatório em HTTPS
        c.setPath("/");
        c.setMaxAge(7 * 24 * 60 * 60); // 7 dias
        response.addCookie(c);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        Cookie c = new Cookie(cookieName, "");
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Utilitários JWT para geração/validação tokens, cookies HTTP-only.
 * Config secret/expiration via properties, HMAC-SHA signing.
 * Usado em auth controllers/filters.
 */
