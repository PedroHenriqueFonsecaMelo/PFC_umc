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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(allowedOrigin));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Restringir apenas headers necessários — não usar "*"
        cfg.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Set-Cookie"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) 
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) 
            .authorizeHttpRequests(auth -> auth
                // ROTAS PÚBLICAS GERAIS
                .requestMatchers("/", "/index", "/home", "/entrar", "/error", "/favicon.ico").permitAll()
                
                // LOGIN E CADASTRO
                .requestMatchers("/clientes/login", "/clientes/novo-cadastro").permitAll() 
                
                // PÁGINAS DE VENDER E VITRINE (A página é pública, mas as ações de venda e compra exigem login)
                .requestMatchers("/vender", "/vitrine").permitAll()
                
                // ADMIN LOGIN - Página pública de login do admin
                .requestMatchers("/admin/login").permitAll()
                
                // PÁGINA ADMIN - Requer autenticação de admin
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                
                // PÁGINA DE HISTÓRIA DO LIVRO (avaliações)
                .requestMatchers("/livros/*/historia").permitAll()
                
                // RECURSOS ESTÁTICOS
                .requestMatchers("/css/**", "/js/**", "/images/**", "/imagens/**", "/cliente/**", "/produto/**", "/static/**").permitAll()
                .requestMatchers("/uploads/**", "/uploads/clientes/**").permitAll()

                // --- CORREÇÃO AQUI: ROTAS DE RECUPERAÇÃO DE SENHA ---
                // Precisam ser permitAll porque o usuário não está autenticado ao usá-las
                .requestMatchers("/clientes/termo", "/clientes/politica", "/clientes/sobre").permitAll()
                .requestMatchers("/clientes/recuperar-senha/**").permitAll()
                .requestMatchers("/clientes/reset-senha/**").permitAll()
                .requestMatchers("/clientes/alterar-senha/**").permitAll()
                
                // TOKEN E CARTEIRA (Libera a página, mas o POST /comprar exige login)
                .requestMatchers("/api/tokens").permitAll() 
                .requestMatchers("/api/livros/carrinho/comprar").authenticated()
                
                // DEBUG E AUTH REST
                .requestMatchers("/auth/**", "/debug").permitAll()
                
                // ROTAS DE ADMIN - Requer autenticação de admin
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                // BLOG - leitura pública, escrita/exclusão apenas admin
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/blog", "/api/blog/**").permitAll()
                .requestMatchers("/api/blog/**").hasAuthority("ADMIN")

                // FÓRUM - leitura pública, criação exige login, moderação exige admin
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/forum", "/forum/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/forum/topicos").permitAll()
                .requestMatchers("/api/forum/topicos/*/deletar").hasAuthority("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/forum/topicos/**").hasAuthority("ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/forum/respostas/**").hasAuthority("ADMIN")
                .requestMatchers("/api/forum/respostas/*/curtir").authenticated()
                .requestMatchers("/api/forum/respostas/*/melhor").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/forum/topicos").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/forum/topicos/*/respostas").authenticated()

                // GAMIFICAÇÃO - ranking público, perfil exige login
                .requestMatchers("/api/gamificacao/ranking").permitAll()
                .requestMatchers("/api/gamificacao/meu-perfil").authenticated()

                // API PÚBLICA - livros e avaliações (visíveis sem login)
                .requestMatchers("/api/livros/todos").permitAll()
                .requestMatchers("/api/avaliacoes/livro/**").permitAll()
                .requestMatchers("/livros/vitrine", "/vender").permitAll()
                
                // ROTAS PRIVADAS
                .requestMatchers("/clientes/meu-perfil", "/clientes/meu-perfil-json").authenticated()
                .requestMatchers("/clientes/minhas-compras").authenticated()
                .requestMatchers("/clientes/sair").authenticated()
                .requestMatchers("/api/tokens/comprar").authenticated()
                .requestMatchers("/api/tokens/historico").authenticated()
                .requestMatchers("/api/tokens/**").authenticated()
                .requestMatchers("/api/pedidos/**").authenticated()
                
                // QUALQUER OUTRA ROTA
                .anyRequest().authenticated()
            );

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