package umc.exs.handler;

import java.net.URI;
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
     * Captura exceções de negócio customizadas (BusinessException)
     * e erros de argumento inválido (IllegalArgumentException).
     */
    @ExceptionHandler({ BusinessException.class, IllegalArgumentException.class })
    public Object handleBusinessException(
            RuntimeException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Regra de negócio violada em {}: {}", request.getRequestURI(), ex.getMessage());

        if (isRestRequest(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", ex.getMessage()));
        }

        // Fluxo MVC: Adiciona mensagem de erro e volta para a página anterior
        redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        return redirectBack(request);
    }

    /**
     * Erros genéricos e inesperados (NullPointer, SQLException, etc).
     */
    @ExceptionHandler(Exception.class)
    public Object handleGenericException(HttpServletRequest request, Exception ex) {
        log.error("Erro crítico em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        if (isRestRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500,
                    "error", "Ocorreu um erro interno inesperado."));
        }

        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("mensagem", "Ocorreu um problema no servidor. Tente novamente mais tarde.");
        return mav;
    }

    // ==========================================================
    // 🛠 MÉTODOS DE APOIO
    // ==========================================================

    private boolean isRestRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/") || uri.startsWith("/auth/");
    }

    /**
     * Lógica de redirecionamento seguro para a página anterior.
     */
    private String redirectBack(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer != null && isSameOrigin(request, referer)) {
            try {
                String path = new URI(referer).getPath();
                return "redirect:" + (path != null && !path.isBlank() ? path : "/");
            } catch (Exception ignored) {
            }
        }
        return "redirect:/";
    }

    private boolean isSameOrigin(HttpServletRequest request, String referer) {
        try {
            URI refererUri = new URI(referer);
            return refererUri.getHost() == null ||
                    refererUri.getHost().equalsIgnoreCase(request.getServerName());
        } catch (Exception e) {
            return false;
        }
    }
}