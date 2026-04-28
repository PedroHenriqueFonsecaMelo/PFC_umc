package umc.exs.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import umc.exs.service.core.interactions.VisitaSiteService;

/**
 * Intercepta requisições GET às páginas web públicas e registra a visita.
 * Ignora chamadas de API, recursos estáticos e painel administrativo.
 */
@Component
@RequiredArgsConstructor
public class VisitaInterceptor implements HandlerInterceptor {

    private final VisitaSiteService visitaSiteService;

    @SuppressWarnings("null")
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"GET".equalsIgnoreCase(request.getMethod()))
            return true;

        String uri = request.getRequestURI();

        boolean isHomepage = uri.equals("/") ||
                uri.equals("/index") ||
                uri.equals("/home");

        if (isHomepage) {
            visitaSiteService.registrarVisita();
        }

        return true;
    }
}
