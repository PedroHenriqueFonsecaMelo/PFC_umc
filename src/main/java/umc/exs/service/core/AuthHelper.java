package umc.exs.service.core;

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
     * @param email usuário
     * @param id cliente
     * @param response cookie
     * @param logAction tipo log
     */
    public void authenticateAndSetCookie(String email, Long id, HttpServletResponse response, String logAction) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(email);
            jwtUtil.addTokenCookie(response, token);

            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            logAuditoriaService.registrarLog(logAction, id, email, "Autenticado JWT gerado");

        } catch (UsernameNotFoundException ignored) {
            logAuditoriaService.registrarLog("AUTENT_FALHA", id, email, "UserDetails não carregado");
        }
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Service helper autenticação cliente/admin.
 * Gera JWT token, seta cookie HTTP-only, Auth SecurityContext.
 * Registra logAuditoria sucesso/falha.
 * Usado controllers login/register.
 */

