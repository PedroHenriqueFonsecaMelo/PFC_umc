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
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

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

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()             
                .getSubject();
    }

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

    public void addTokenCookie(HttpServletResponse response, String token) {
        Cookie c = new Cookie(cookieName, token);
        c.setHttpOnly(true);
        c.setSecure(false); // Defina como true em produção (HTTPS)
        c.setPath("/");
        response.addCookie(c);
    }

    public void clearJwtCookie(HttpServletResponse response) {
        Cookie c = new Cookie(cookieName, "");
        c.setHttpOnly(true);
        c.setPath("/");
        c.setMaxAge(0);
        response.addCookie(c);
    }
}