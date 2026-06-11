package umc.exs.handler;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler global de exceções da aplicação; intercepta erros de todos os controllers.
 * Retorna JSON para requisições REST (/api/, /auth/) ou redireciona para páginas de erro para requisições web.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Trata exceções de regras de negócio violadas (BusinessException).
     * Retorna HTTP 400 Bad Request com a mensagem da regra violada.
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("Business error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", ex.getMessage()));
        }

        response.setStatus(400);
        return "error/400";
    }

    /**
     * Trata excesso de requisições (TooManyRequestsException) disparado pelo Rate Limit.
     * Retorna HTTP 429 Too Many Requests com mensagem de erro.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public Object handle429(TooManyRequestsException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("429 error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.status(429).body(Map.of(
                    "status", 429,
                    "error", ex.getMessage()));
        }

        response.setStatus(429);
        return "error/429";
    }

    /**
     * Trata recursos não encontrados (NoResourceFoundException).
     * Retorna HTTP 404 Not Found com mensagem padronizada.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handle404(NoResourceFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("404 error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not found"));
        }

        response.setStatus(404);
        return "error/404";
    }

    /**
     * Trata falhas de autenticação por credenciais inválidas (BadCredentialsException).
     * Retorna HTTP 401 Unauthorized com mensagem padronizada.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public Object handle401(BadCredentialsException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("401 error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized"));
        }

        response.setStatus(401);
        return "error/401";
    }

    /**
     * Trata qualquer exceção não prevista pelos handlers anteriores.
     * Retorna HTTP 500 Internal Server Error com mensagem genérica para não expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public Object handle500(Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.error("500 error", ex);

        if (isRest(request)) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", 500,
                    "error", "Internal error"));
        }

        response.setStatus(500);
        return "error/500";
    }

    /**
     * Verifica se a requisição é REST pelo URI (/api/, /auth/) ou pelo cabeçalho Accept.
     * Usado para decidir entre retornar JSON ou redirecionar para página de erro Thymeleaf.
     */
    private boolean isRest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return uri.startsWith("/api/")
                || uri.startsWith("/auth/")
                || (accept != null && accept.contains("application/json"));
    }
}
