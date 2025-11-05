package umc.exs.backstage.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long validityMs;

    public JwtUtil(
            @Value("${jwt.secret:ChangeThisSecretToAStrongOne_UseEnv_256bits!}") String secret,
            @Value("${jwt.validity-ms:86400000}") long validityMs // 1 dia
    ) {
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

    // 1️⃣ Token padrão: username ou CPF
    public String generateToken(String subject) {
        return buildToken(subject);
    }

    // 2️⃣ Token com UserDetails
    public String generateToken(UserDetails userDetails) {
        if (userDetails == null) return null;
        return buildToken(userDetails.getUsername());
    }

    // 3️⃣ Token com email + id
    public String generateToken(String email, Long id) {
        if (email == null || id == null) return null;
        String subject = email + ":" + id;
        return buildToken(subject);
    }

    // ==============================
    // 🔹 Método interno de criação de token
    // ==============================
    private String buildToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMs);

        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    // ==============================
    // 🔹 Extração de claims
    // ==============================
    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parseClaims(token);
        return claims == null ? null : resolver.apply(claims);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    // ==============================
    // 🔹 Extração específica
    // ==============================

    // Padrão: retorna subject bruto (username, CPF ou email:id)
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extração de email e id, se token tiver o formato "email:id"
    public Pair<String, Long> extractEmailAndId(String token) {
        String subject = extractSubject(token);
        if (subject == null || !subject.contains(":")) return null;

        String[] parts = subject.split(":");
        if (parts.length != 2) return null;

        try {
            return Pair.of(parts[0], Long.valueOf(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String extractEmail(String token) {
        Pair<String, Long> pair = extractEmailAndId(token);
        return pair != null ? pair.getFirst() : null;
    }

    public Long extractId(String token) {
        Pair<String, Long> pair = extractEmailAndId(token);
        return pair != null ? pair.getSecond() : null;
    }

    public String extractUsername(String token) {
        String subject = extractSubject(token);
        if (subject == null) return null;

        // Se for email:id, retorna só o email
        if (subject.contains(":")) {
            Pair<String, Long> pair = extractEmailAndId(token);
            return pair != null ? pair.getFirst() : null;
        }

        // Caso contrário, retorna o subject como username ou CPF
        return subject;
    }   

    // ==============================
    // 🔹 Validação
    // ==============================
    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        if (claims == null) return false;

        Date exp = claims.getExpiration();
        return exp == null || exp.after(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        if (token == null || userDetails == null) return false;

        String subject = extractSubject(token);
        if (subject == null) return false;

        // Verifica username ou CPF diretamente
        if (subject.equals(userDetails.getUsername())) {
            return validateToken(token);
        }

        // Caso seja email:id
        Pair<String, Long> pair = extractEmailAndId(token);
        return pair != null && pair.getFirst().equals(userDetails.getUsername()) && validateToken(token);
    }
}
