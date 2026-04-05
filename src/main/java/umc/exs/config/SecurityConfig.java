package umc.exs.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import umc.exs.security.RateLimitFilter;
import umc.exs.security.CustomAccessDeniedHandler;

@Configuration
public class SecurityConfig {

        @Value("${app.allowed-origin:http://localhost:5173}")
        private String allowedOrigin;

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.setAllowedOrigins(List.of(allowedOrigin));
                cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                cfg.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));
                cfg.setExposedHeaders(List.of("Set-Cookie"));
                cfg.setAllowCredentials(true);
                cfg.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", cfg);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                        JwtRequestFilter jwtRequestFilter,
                        RateLimitFilter rateLimitFilter,
                        CustomAccessDeniedHandler accessDeniedHandler) throws Exception {

                // 1. Recursos Estáticos e Páginas Iniciais
                final String[] STATIC_AND_PUBLIC_PAGES = {
                                "/", "/index", "/home", "/entrar", "/error", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/cliente/**", "/produto/**", "/static/**",
                                "/uploads/**",
                                "/vender", "/vitrine", "/livros/vitrine"
                };

                // 2. Fluxo de Autenticação e Recuperação
                final String[] AUTH_FLOW = {
                                "/clientes/login", "/clientes/novo-cadastro",
                                "/clientes/recuperar-senha/**", "/clientes/reset-senha/**", "/clientes/alterar-senha/**"
                };

                // 3. APIs de Leitura Pública ou Utilitários
                final String[] PUBLIC_APIS = {
                                "/api/tokens", "/api/livros/todos", "/api/avaliacoes/livro/**",
                                "/api/gamificacao/ranking", "/auth/**", "/debug"
                };

                http
                                .headers(headers -> headers
                                                .contentSecurityPolicy(
                                                                csp -> csp.policyDirectives("default-src 'self'"))
                                                .frameOptions(frame -> frame.deny())
                                                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)
                                                                .includeSubDomains(true))
                                                .contentTypeOptions(cto -> {
                                                }))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Aplicando os agrupamentos
                                                .requestMatchers(STATIC_AND_PUBLIC_PAGES).permitAll()
                                                .requestMatchers(AUTH_FLOW).permitAll()
                                                .requestMatchers(PUBLIC_APIS).permitAll()

                                                // --- ROTAS ESPECÍFICAS QUE FORAM RECUPERADAS ---
                                                .requestMatchers("/livros/*/historia").permitAll()

                                                // BLOG: Público lê, Admin escreve
                                                .requestMatchers(HttpMethod.GET, "/api/blog", "/api/blog/**")
                                                .permitAll()

                                                // --- REGRAS DE AUTORIDADE (ADMIN) ---
                                                .requestMatchers("/admin/**", "/api/admin/**", "/api/blog/**")
                                                .hasAuthority("ADMIN")

                                                // --- REGRAS DE AUTENTICAÇÃO (CLIENTE LOGADO) ---
                                                .requestMatchers(
                                                                "/clientes/meu-perfil", "/clientes/meu-perfil-json",
                                                                "/clientes/minhas-compras",
                                                                "/clientes/sair",
                                                                "/api/tokens/comprar", "/api/tokens/historico",
                                                                "/api/tokens/**",
                                                                "/api/pedidos/**", "/api/livros/carrinho/comprar",
                                                                "/api/gamificacao/meu-perfil")
                                                .authenticated()

                                                // Qualquer outra coisa não listada
                                                .anyRequest().authenticated());

                http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
                return cfg.getAuthenticationManager();
        }
}