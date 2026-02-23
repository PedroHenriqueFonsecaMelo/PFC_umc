package umc.exs.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import umc.exs.security.JwtRequestFilter;

@Configuration
public class SecurityConfig {

    @Value("${app.allowed-origin:http://localhost:5173}")
    private String allowedOrigin;

    /**
     * Configuração de CORS para permitir que o Frontend (ex: React/Vue ou o próprio navegador)
     * consiga enviar cookies e headers para este servidor.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true); // Obrigatório para cookies JWT funcionarem
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    /**
     * Define a corrente de filtros de segurança e as permissões de URL.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Desabilitado para uso de Tokens JWT
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Não cria sessão no servidor
            .authorizeHttpRequests(auth -> auth
                // ROTAS PÚBLICAS: Acesso livre para qualquer um
                .requestMatchers("/", "/index", "/home", "/error", "/favicon.ico").permitAll()
                .requestMatchers("/clientes/login", "/clientes/novo-cadastro").permitAll() 
                .requestMatchers("/auth/**", "/debug").permitAll()
                
                // RECURSOS ESTÁTICOS: CSS, JS e Imagens devem ser públicos
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.io/**", "/cliente/**").permitAll()
                .requestMatchers("/clientes/tokens").permitAll() // Permite carregar o HTML
                
                // ROTAS PRIVADAS: Somente usuários com JWT válido entram aqui
                .requestMatchers("/clientes/meu-perfil").authenticated()
                .requestMatchers("/clientes/sair").authenticated()
                
                // QUALQUER OUTRA ROTA: Exige autenticação por padrão
                .anyRequest().authenticated()
            );

        // Insere o filtro JWT antes do filtro de autenticação padrão do Spring
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Algoritmo de Hashing para senhas (BCrypt é o padrão da indústria)
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}