package umc.exs.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.apache.catalina.connector.Connector;

/**
 * Configura o redirecionamento automático de HTTP (porta 8080) para HTTPS
 * (porta 8443) no ambiente local; ativa apenas com o profile "local".
 */
@Configuration
@Profile("local")
public class HttpsRedirectConfig {

    /**
     * Adiciona um conector HTTP secundário ao Tomcat que redireciona
     * automaticamente para a porta HTTPS configurada.
     */
    @Bean
    public TomcatServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        // Adiciona conector HTTP na porta 8080
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        tomcat.addAdditionalTomcatConnectors(connector);

        return tomcat;
    }
}
