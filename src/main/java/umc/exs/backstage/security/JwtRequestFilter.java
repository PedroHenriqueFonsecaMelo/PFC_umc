package umc.exs.backstage.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    public static Logger getLogger() {
        return logger;
    }

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    private static final List<String> PUBLIC_PREFIXES = List.of(
        "/auth", "/auth/", "/clientes/cadastro", "/clientes/cadastro/",
        "/css/", "/js/", "/images/", "/webjars/", "/error", "/login"
    );

    @SuppressWarnings("unused")
    private boolean isPublic(String path) {
        if (path == null) return false;
        for (String p : PUBLIC_PREFIXES) {
            if (path.equals(p) || path.startsWith(p)) return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        logger.debug("Incoming request [{}] Cookies: {}", path,
                request.getCookies() == null ? "none" : Arrays.stream(request.getCookies()).map(Cookie::getName).collect(Collectors.joining(",")));

        String token = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("Found Bearer token in Authorization header");
        } else if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("token".equals(c.getName()) && c.getValue() != null && !c.getValue().isBlank()) {
                    token = c.getValue();
                    logger.debug("Found token cookie (len={})", token.length());
                    break;
                }
            }
        } else {
            logger.debug("No cookies present");
        }

        if (token != null) {
            try {
                String username = jwtUtil.extractUsername(token);
                logger.debug("Token subject: {}", username);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails ud = userDetailsService.loadUserByUsername(username);
                    if (jwtUtil.validateToken(token, ud)) {
                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        logger.debug("SecurityContext populated for {}", username);
                    } else {
                        logger.debug("JWT validation failed");
                    }
                }
            } catch (UsernameNotFoundException ex) {
                logger.warn("JWT processing failed: {}", ex.getMessage());
            }
        }

        chain.doFilter(request, response);
    }
}
