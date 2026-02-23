package umc.exs.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain chain) 
            throws ServletException, IOException {

        // Resolve o token (procura em cookies e depois no header)
        String token = resolveTokenFromRequest(request);

        // Se encontrou um token, valida a assinatura e o tempo de expiração
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            
            // Se o username é válido e ainda não há ninguém autenticado nesta requisição
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Carrega os dados do usuário do banco de dados
                UserDetails ud = userDetailsService.loadUserByUsername(username);
                
                // Cria o objeto de autenticação do Spring Security
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                
                // Define o usuário como "Autenticado" no contexto do sistema
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // Continua o fluxo da requisição para o próximo filtro ou para o Controller
        chain.doFilter(request, response);
    }

    /**
     * Lógica para extrair o token da requisição sem o uso de Streams.
     */
    private String resolveTokenFromRequest(HttpServletRequest request) {
        // 1. Tenta buscar nos Cookies (Padrão para Navegador/Thymeleaf)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("token".equalsIgnoreCase(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Tenta buscar no Header Authorization (Padrão para API/Postman)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove a palavra "Bearer " e pega o token
        }

        return null;
    }
}