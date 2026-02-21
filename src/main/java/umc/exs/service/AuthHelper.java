package umc.exs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.log.LogAuditoriaService;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;

@Service
public class AuthHelper {

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    public void authenticateAndSetCookie(String email, Long id, HttpServletResponse response, String logAction) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(email);
            jwtUtil.addTokenCookie(response, token);

            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            logAuditoriaService.registrarLog(logAction, id, email, "Usuário autenticado e token JWT gerado.");

        } catch (UsernameNotFoundException ignored) {
            logAuditoriaService.registrarLog("AUTENTICACAO_INTERNA_FALHA", id, email, "Falha ao carregar UserDetails.");
        }
    }
}
