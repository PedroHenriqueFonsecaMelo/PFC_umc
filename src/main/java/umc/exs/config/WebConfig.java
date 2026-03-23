package umc.exs.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("local")
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configura handler recursos estáticos para uploads.
     * Mapeia /uploads/** → diretório ./uploads/ local (perfil local).
     * 
     * @param registry recursos web
     */
    @SuppressWarnings("null")
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Define o caminho absoluto para a pasta de uploads
        Path uploadDir = Paths.get("./uploads");
        String uploadPath = uploadDir.toFile().getAbsolutePath();

        // Mapeia a URL /uploads/** para o diretório físico
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/" + uploadPath + "/");
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Config WebMvc local (perfil 'local').
 * Serve arquivos uploads/livros/** estáticos.
 * Caminho absoluto ./uploads.
 */
