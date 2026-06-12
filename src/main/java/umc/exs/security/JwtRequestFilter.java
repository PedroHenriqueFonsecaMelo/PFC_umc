package umc.exs.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro JWT executado uma vez por requisição que intercepta todas as requisições HTTP,
 * valida o token JWT presente no cookie HTTP-only ou no header Authorization e,
 * quando válido, configura a autenticação do usuário no SecurityContext do Spring Security.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;

    @Value("${jwt.cookie.name:token}")
    private String cookieName;

    /**
     * Ignora requisições de recursos estáticos para evitar processamento desnecessário.
     * Para as demais requisições, extrai e valida o JWT e, se válido, carrega os dados
     * do usuário e autentica no SecurityContext antes de prosseguir na cadeia de filtros.
     */
    @Override
    public void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/images/") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/produto/") ||
                requestURI.startsWith("/cliente/") ||
                requestURI.startsWith("/uploads/") ||
                requestURI.endsWith(".js") ||
                requestURI.endsWith(".css") ||
                requestURI.endsWith(".ico") ||
                requestURI.endsWith(".png") ||
                requestURI.endsWith(".jpg") ||
                requestURI.endsWith(".woff2")) {
            chain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails ud = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(ud, null,
                            ud.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    System.out.println("[JWT] token OK uri=" + requestURI + " username=" + username + " authorities=" + ud.getAuthorities());
                } catch (Exception e) {
                    System.out.println("[JWT] falha ao carregar usuario uri=" + requestURI + " username=" + username + " erro=" + e.getMessage());
                }
            }
        } else {
            System.out.println("[JWT] token NULO/INV uri=" + requestURI);
        }

        chain.doFilter(request, response);
    }

    /**
     * Extrai o token JWT da requisição, buscando primeiro no cookie HTTP-only configurado
     * e utilizando o header Authorization com prefixo Bearer como fallback para clientes de API.
     */
    private String resolveToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName() != null && cookie.getName().equalsIgnoreCase(cookieName)) {
                    return cookie.getValue();
                }

            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

