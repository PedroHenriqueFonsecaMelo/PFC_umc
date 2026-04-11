package umc.exs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class VisitaInterceptorConfig implements WebMvcConfigurer {

    private final VisitaInterceptor visitaInterceptor;

    @SuppressWarnings("null")
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(visitaInterceptor);
    }
}
