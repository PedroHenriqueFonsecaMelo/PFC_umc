package umc.exs.backstage.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    private static final List<String> PUBLIC_PREFIXES = List.of(
        "/auth", "/auth/", "/clientes/cadastro", "/clientes/cadastro/",
        "/css/", "/js/", "/images/", "/webjars/", "/error", "/login"
    );

    private boolean isPublic(String path) {
        if (path == null) return false;
        for (String p : PUBLIC_PREFIXES) {
            if (path.equals(p) || path.startsWith(p)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            logger.debug("Found Bearer token in Authorization header");
        } else {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                logger.debug("Cookies present: {}", Arrays.stream(cookies).map(Cookie::getName).collect(Collectors.joining(",")));
                Optional<Cookie> cookie = Arrays.stream(cookies).filter(c -> "token".equals(c.getName())).findFirst();
                if (cookie.isPresent()) {
                    token = cookie.get().getValue();
                    logger.debug("Found token in cookie");
                } else {
                    logger.debug("token cookie not found among cookies");
                }
            } else {
                logger.debug("No cookies present");
            }
        }

        if (token != null && !token.isBlank()) {
            String username = null;
            try {
                username = jwtUtil.extractUsername(token);
                logger.debug("Token subject: {}", username);
            } catch (Exception e) {
                logger.warn("Failed extracting username from token: {}", e.getMessage());
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    boolean valid = jwtUtil.validateToken(token, userDetails);
                    logger.debug("Token valid: {}", valid);
                    if (valid) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        logger.debug("SecurityContext populated for {}", username);
                    }
                } catch (Exception e) {
                    logger.warn("Error validating token or loading user: {}", e.getMessage());
                }
            }
        } else {
            logger.debug("No token provided for request {}", path);
        }

        filterChain.doFilter(request, response);
    }
}
