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
        public SecurityFilterChain filterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter,
                        RateLimitFilter rateLimitFilter,
                        umc.exs.security.JwtAuthenticationEntryPoint entryPoint) throws Exception {
                http
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdnjs.cloudflare.com; "
                                                                                +
                                                                                "font-src 'self' https://fonts.gstatic.com https://cdnjs.cloudflare.com; "
                                                                                +
                                                                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; "
                                                                                +
                                                                                "connect-src 'self' https://www.googleapis.com https://openlibrary.org https://viacep.com.br; "
                                                                                +
                                                                                "img-src 'self' data: https:;"))
                                                .frameOptions(frame -> frame.deny())
                                                .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000)
                                                                .includeSubDomains(true))
                                                .contentTypeOptions(cto -> {
                                                }))
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // 1. Recursos Estáticos (CSS, JS, Imagens)
                                                .requestMatchers(STATIC_RESOURCES).permitAll()

                                                // 2. Rotas Públicas (Páginas Web e APIs de consulta)
                                                .requestMatchers(PUBLIC_ROUTES).permitAll()

                                                // 3. Rotas de Blog e Fórum (Consulta pública, escrita protegida)
                                                .requestMatchers(HttpMethod.GET, "/api/blog/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/blog/*/curtir").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/blog/*/comentarios").authenticated()
                                                .requestMatchers("/api/blog/**").hasAuthority("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/forum/**", "/api/forum/topicos")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/forum/topicos/**").authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/forum/**")
                                                .hasAuthority("ADMIN")

                                                // 4. Rotas da Central de Opiniões (Consulta pública, Salvar
                                                // autenticado)
                                                .requestMatchers(HttpMethod.GET, "/api/avaliacoes/livro/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/avaliacoes/salvar")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/avaliacoes/admin/**")
                                                .hasAuthority("ADMIN")

                                                // 5. Webhook Mercado Pago e simulação de pagamento
                                                .requestMatchers(HttpMethod.POST, "/api/tokens/webhook").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/tokens/simular-webhook/**").permitAll()

                                                // 6. Rotas de Admin e Rotas Autenticadas Genéricas
                                                .requestMatchers(ADMIN_ROUTES).hasAuthority("ADMIN")
                                                .requestMatchers(AUTHENTICATED_ROUTES).authenticated()

                                                // 7. Qualquer outra rota exige login
                                                .anyRequest().authenticated())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(entryPoint))
                                .logout(logout -> logout
                                                .logoutUrl("/clientes/sair")
                                                .deleteCookies("token")
                                                .logoutSuccessUrl("/").permitAll());

                // Adição dos Filtros de JWT e Rate Limit
                http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
                http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

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

        // Rotas que qualquer um pode acessar
        private static final String[] PUBLIC_ROUTES = {
                        "/", "/index", "/home", "/entrar", "/error", "/favicon.ico",
                        "/clientes/login", "/clientes/novo-cadastro",
                        "/vender", "/vitrine", "/admin/login",
                        "/livros/*/historia", "/livros/**", "/api/tokens",
                        "/auth/**", "/debug", "/api/gamificacao/ranking",
                        "/api/livros/todos", "/api/avaliacoes/livro/**",
                        "/livros/vitrine", "/clientes/termo", "/clientes/politica", "/clientes/sobre",
                        "/clientes/recuperar-senha/**", "/clientes/reset-senha/**", "/clientes/alterar-senha/**",
                        "/blog", "/blog/**"
        };

        // Recursos estáticos (CSS, JS, Imagens)
        private static final String[] STATIC_RESOURCES = {
                        "/css/**", "/js/**", "/images/**", "/imagens/**", "/cliente/**",
                        "/produto/**", "/static/**", "/uploads/**"
        };

        // Rotas exclusivas para ADMIN
        private static final String[] ADMIN_ROUTES = {
                        "/admin/**", "/api/admin/**"
        };

        // Rotas que exigem apenas que o usuário esteja logado (User ou Admin)
        private static final String[] AUTHENTICATED_ROUTES = {
                        "/api/livros/carrinho/comprar", "/api/gamificacao/meu-perfil",
                        "/clientes/meu-perfil", "/clientes/meu-perfil-json",
                        "/clientes/minhas-compras", "/clientes/sair",
                        "/api/tokens/comprar", "/api/tokens/historico", "/api/tokens/**",
                        "/api/pedidos/**", "/api/forum/respostas/*/curtir", "/api/forum/respostas/*/melhor",
                        "/api/lista-desejos", "/api/lista-desejos/**",
                        "/clientes/lista-desejos"
        };
}