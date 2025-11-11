package umc.exs.backstage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.backstage.log.LogAuditoriaService;
import umc.exs.backstage.security.JwtUserDetailsService;
import umc.exs.backstage.security.JwtUtil;

@Service
public class AuthHelper {

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    /**
     * Autentica o usuário, gera o token, adiciona o cookie e registra o log.
     */
    public void authenticateAndSetCookie(String email, Long id, HttpServletResponse response, String logAction) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(email);
            
            // 1. Adiciona o cookie (usando o helper)
            jwtUtil.addTokenCookie(response, token);

            // 2. Define o contexto de segurança
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // 3. Log de sucesso
            logAuditoriaService.registrarLog(logAction, id, email, "Usuário autenticado e token JWT gerado.");
            
        } catch (UsernameNotFoundException ignored) {
            // Se o userDetails não for encontrado após uma operação bem-sucedida (ex: cadastro)
            logAuditoriaService.registrarLog("AUTENTICACAO_INTERNA_FALHA", id, email, "Falha ao carregar UserDetails.");
        }
    }
}
