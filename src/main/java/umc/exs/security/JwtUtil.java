package umc.exs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import io.jsonwebtoken.JwtException;

@Component
public class JwtUtil {

    @Value("${jwt.secret:changeitchangeitchangeitchangeit}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expirationMs;

    @Value("${jwt.cookie.name:token}")
    private String cookieName;

    @Value("${jwt.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${jwt.cookie.samesite:Lax}")
    private String sameSitePolicy;

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

    public String generateToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", roles)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs)) // Sincronizado com properties
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

    /**
     * ADICIONA COOKIE UTILIZANDO A API RESPONSECOOKIE DO SPRING.
     * Corrige de forma definitiva o bloqueio do Chrome em ambiente local.
     */
    public void addTokenCookie(HttpServletResponse response, String token) {
        // Converte os milissegundos de expiração para segundos (padrão Max-Age)
        long maxAgeSeconds = expirationMs / 1000;

        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
                .httpOnly(true)
                .secure(secureCookie)        // Se tornará 'false' no localhost
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSitePolicy)    // Se tornará 'Lax' no localhost
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * LIMPA O COOKIE DE FORMA COMPATÍVEL COM AS NOVAS DIRETIVAS.
     */
    public void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/")
                .maxAge(0)
                .sameSite(sameSitePolicy)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
