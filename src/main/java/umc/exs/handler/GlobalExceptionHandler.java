package umc.exs.handler;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Erros de regra de negócio (IllegalArgumentException).
     * Se for REST → JSON 400. Se for MVC → redireciona com flash.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        if (isRestRequest(request)) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", ex.getMessage()));
        }

        redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        String referer = request.getHeader("Referer");
        // Proteção contra Open Redirect: só redireciona para caminhos internos
        if (referer != null && isSameOrigin(request, referer)) {
            try {
                java.net.URI uri = new java.net.URI(referer);
                String path = uri.getPath();
                return "redirect:" + (path != null && !path.isBlank() ? path : "/");
            } catch (Exception ignored) {
            }
        }
        return "redirect:/";
    }

    /**
     * Erros genéricos inesperados.
     * Se for REST → JSON 500. Se for MVC → página de erro.
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(HttpServletRequest req, Exception ex) {
        log.error("Erro inesperado em {}: {}", req.getRequestURL(), ex.getMessage(), ex);

        if (isRestRequest(req)) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno. Tente novamente mais tarde."));
        }

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("mensagem", "Ocorreu um erro interno. Tente novamente mais tarde.");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return mav;
    }

    /** Considera REST se a URI começa com /api/ ou /auth/ */
    private boolean isRestRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") || uri.startsWith("/auth/");
    }

    /** Valida que o Referer pertence ao mesmo host — previne Open Redirect */
    private boolean isSameOrigin(HttpServletRequest request, String referer) {
        try {
            java.net.URI refererUri = new java.net.URI(referer);
            String refererHost = refererUri.getHost();
            String serverHost = request.getServerName();
            return refererHost != null && refererHost.equalsIgnoreCase(serverHost);
        } catch (Exception e) {
            return false;
        }
    }
}
