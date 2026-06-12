package umc.exs.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handler personalizado que trata tentativas de acesso a recursos sem permissão.
 * Retorna HTTP 403 com corpo JSON estruturado em vez da página de erro padrão do Spring,
 * garantindo respostas consistentes para clientes da API.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Chamado automaticamente pelo Spring Security quando um usuário autenticado tenta
     * acessar um recurso para o qual não possui permissão.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"error\": \"Access denied\"}");
    }
}
