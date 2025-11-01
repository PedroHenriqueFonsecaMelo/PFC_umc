package umc.exs.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtParser;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long validityMs;
    // O JwtParser é imutável e thread-safe, então o criamos apenas uma vez.
    private final JwtParser parser;

    /**
     * Construtor que inicializa a chave secreta e o parser de JWT.
     * @param secret A chave secreta baseada em String (configurada via Spring @Value).
     * @param validityMs O tempo de validade do token em milissegundos.
     */
    public JwtUtil(
            @Value("${jwt.secret:ChangeThisSecretToAStrongOne_UseEnvOrVault_256bits!}") String secret,
            @Value("${jwt.validity-ms:86400000}") long validityMs) {

        // 1. Gera uma chave HMAC SHA a partir da String secreta configurada
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;

        // 2. Constrói o parser de forma imutável, definindo a chave para verificação (verifyWith)
        // Isso corrige o método obsoleto setSigningKey().
        this.parser = Jwts.parser()
                .verifyWith(this.key) // Define a chave para validar a assinatura
                .build(); // Constrói o parser
    }

    /**
     * Gera um token JWT para um determinado nome de usuário.
     * * @param username O nome de usuário a ser colocado no 'subject' (assunto) do token.
     * @return O token JWT compactado (String).
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMs); // Define a data de expiração

        return Jwts.builder()
                .subject(username) // Define o 'subject' (usuário)
                .issuedAt(now) // Data de emissão
                .expiration(exp) // Data de expiração
                .signWith(key) // Assina o token com a chave secreta
                .compact(); // Compacta e retorna o token
    }

    /**
     * Obtém o nome de usuário (subject) do token JWT.
     * * @param token O token JWT.
     * @return O nome de usuário (subject) ou null se o token for inválido.
     */
    public String getUsernameFromToken(String token) {
        try {
            // Usa o parser pré-construído e o novo método parseSignedClaims()
            Claims claims = parser.parseSignedClaims(token) // Analisa e valida claims de um JWS
                    .getPayload(); // Novo método para extrair o corpo/claims (substitui getBody())

            return claims.getSubject(); 
        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Erro ao obter nome de usuário do token: " + e.getMessage());
            return null; // Se houver erro de parsing ou validação, retorna null
        }
    }

    /**
     * Valida o token JWT, verificando sua assinatura e validade (expiração).
     * * @param token O token JWT a ser validado.
     * @return true se o token for válido e não estiver expirado, false caso contrário.
     */
    public boolean validateToken(String token) {
        try {
            // Se a linha abaixo for executada sem exceções, o token é válido e não expirou.
            parser.parseSignedClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {
            System.err.println("Token inválido: " + e.getMessage());
            return false; // Retorna false se a validação falhar
        }
    }

    /**
     * Obtém o ID de usuário do token (assumindo que o subject é o ID do usuário).
     * Nota: Este método é idêntico a getUsernameFromToken, pois ambos extraem o 'subject'.
     * * @param token O token JWT.
     * @return O ID de usuário (subject) ou null se o token for inválido.
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}