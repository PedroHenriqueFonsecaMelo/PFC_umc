package umc.exs.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.*;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String uri = request.getRequestURI();

        if (uri.startsWith("/api/")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("""
                        {
                          "status": 401,
                          "error": "Não autenticado"
                        }
                    """);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
