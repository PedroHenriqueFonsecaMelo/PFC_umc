package umc.exs.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Ponto de entrada de autenticação JWT que trata tentativas de acesso sem autenticação.
 * Retorna HTTP 401 com corpo JSON para requisições de API (prefixo /api/) e erro padrão
 * do servlet para demais requisições web.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Chamado pelo Spring Security quando uma requisição não autenticada tenta acessar
     * um recurso protegido. Diferencia o tipo de resposta com base no URI: requisições
     * de API recebem JSON estruturado, enquanto requisições web recebem o erro padrão HTTP.
     */
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
