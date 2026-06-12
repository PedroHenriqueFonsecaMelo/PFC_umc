package umc.exs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

/**
 * Registra o VisitaInterceptor no Spring MVC para que ele intercepte todas as
 * requisições da aplicação.
 */
@Configuration
@RequiredArgsConstructor
public class VisitaInterceptorConfig implements WebMvcConfigurer {

    private final VisitaInterceptor visitaInterceptor;

    /** Adiciona o interceptor de visitas ao registro do Spring MVC. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitaInterceptor);
    }
}
