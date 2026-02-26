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
        cfg.setAllowedHeaders(List.of("*"));
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
                .requestMatchers("/", "/index", "/home", "/error", "/favicon.ico").permitAll()
                
                // LOGIN E CADASTRO
                .requestMatchers("/clientes/login", "/clientes/novo-cadastro").permitAll() 
                
                // PÁGINAS DE VENDER E VITRINE (A página é pública, mas as ações de venda e compra exigem login)
                .requestMatchers("/vender", "/vitrine").permitAll()
                
                // RECURSOS ESTÁTICOS
                .requestMatchers("/css/**", "/js/**", "/images/**", "/cliente/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // --- CORREÇÃO AQUI: ROTAS DE RECUPERAÇÃO DE SENHA ---
                // Precisam ser permitAll porque o usuário não está autenticado ao usá-las
                .requestMatchers("/clientes/recuperar-senha/**").permitAll()
                .requestMatchers("/clientes/reset-senha/**").permitAll()
                .requestMatchers("/clientes/alterar-senha/**").permitAll()
                
                // TOKEN E CARTEIRA (Libera a página, mas o POST /comprar exige login)
                .requestMatchers("/clientes/tokens").permitAll() 
                
                // DEBUG E AUTH REST
                .requestMatchers("/auth/**", "/debug").permitAll()
                
                // ROTAS PRIVADAS
                .requestMatchers("/clientes/meu-perfil", "/clientes/meu-perfil-json").authenticated()
                .requestMatchers("/clientes/sair").authenticated()
                .requestMatchers("/clientes/tokens/comprar").authenticated()
                .requestMatchers("/clientes/tokens/historico").authenticated()
                
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