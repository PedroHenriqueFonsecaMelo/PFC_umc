package umc.exs.service.core.control;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.log.LogAuditoriaService;

@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final LogAuditoriaService logAuditoriaService;

    /**
     * Autentica usuário e define cookie JWT.
     * Carrega UserDetails, gera token, set Auth context.
     * Registra log auditoria ação específica.
     * 
     * @param email     usuário
     * @param id        cliente
     * @param response  cookie
     * @param logAction tipo log
     */
    public void authenticateAndSetCookie(String email, Long id, HttpServletResponse response, String logAction) {
        try {
            // 1. Carrega as permissões (Roles/Authorities) do banco
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            
            // 2. Gera o Token e já injeta no Header de resposta (Cookie)
            String token = jwtUtil.generateToken(email);
            jwtUtil.addTokenCookie(response, token);

            // 3. Informa ao Spring Security que este usuário está autenticado nesta thread
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 4. Auditoria
            logAuditoriaService.registrarLog(logAction, id, email, "Autenticação via JWT concluída com sucesso.");

        } catch (UsernameNotFoundException e) {
            logAuditoriaService.registrarLog("AUTENT_FALHA", id, email, "Falha ao carregar detalhes do usuário: " + e.getMessage());
        }
    }
    public void addTokenCookie(HttpServletResponse response, String token) {
        jwtUtil.addTokenCookie(response, token);
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service helper autenticação cliente/admin.
 * Gera JWT token, seta cookie HTTP-only, Auth SecurityContext.
 * Registra logAuditoria sucesso/falha.
 * Usado controllers login/register.
 */
