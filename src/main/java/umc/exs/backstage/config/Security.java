package umc.exs.backstage.config;


public class Security {
    //Permiti acesso a páginas estáticas e à página de login sem autenticação do Spring Security
    /* 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/static/**", "/", 
                "/index.html", "**.js","**.css", 
                "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }*/
}