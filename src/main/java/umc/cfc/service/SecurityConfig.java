// src/main/java/umc/cfc/config/SecurityConfig.java

package umc.cfc.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desabilita a proteção CSRF para que as requisições POST do seu HTML funcionem.
            // Apenas faça isso se você não estiver usando CSRF tokens.
            .csrf(csrf -> csrf.disable())

            // Permite que as requisições para a pasta /auth/ sejam acessadas sem autenticação.
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/static/**", "/", "/index.html", "**.js","**.css").permitAll()
                .anyRequest().authenticated()
            );

        // Retorna a cadeia de filtros de segurança configurada.
        return http.build();
    }
}