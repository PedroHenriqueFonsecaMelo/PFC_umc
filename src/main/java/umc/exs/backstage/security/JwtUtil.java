package umc.exs.backstage.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long validityMs;

    public JwtUtil(
            @Value("${jwt.secret:ChangeThisSecretToAStrongOne_UseEnv_256bits!}") String secret,
            @Value("${jwt.validity-ms:86400000}") long validityMs // 1 dia
    ) {
        // Garante que a chave tenha 256 bits
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret precisa ter pelo menos 256 bits (32 caracteres)");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityMs = validityMs;
    }

    // ==============================
    // 🔹 Geração de token
    // ==============================
    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    // ==============================
    // 🔹 Extração de username
    // ==============================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // ==============================
    // 🔹 Extração genérica
    // ==============================
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parseClaims(token);
        return claims == null ? null : resolver.apply(claims);
    }

    // ==============================
    // 🔹 Parse de Claims
    // ==============================
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key) // valida assinatura
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null; // token inválido, expirado ou assinado incorretamente
        }
    }

    // ==============================
    // 🔹 Validação completa
    // ==============================
    public boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || userDetails == null) return false;
        Claims claims = parseClaims(token);
        if (claims == null) return false;
        String subject = claims.getSubject();
        Date exp = claims.getExpiration();
        return subject != null
                && subject.equals(userDetails.getUsername())
                && (exp == null || exp.after(new Date()));
    }

    // ==============================
    // 🔹 Validação simples
    // ==============================
    public boolean validateToken(String token) {
        return parseClaims(token) != null;
    }

    // ==============================
    // 🔹 Extração de userId
    
    public  String getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return null;
        return claims.get("userId", String.class);
    }
}
