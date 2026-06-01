package umc.exs.service.core.control;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

@Service
@RequiredArgsConstructor
public class AuthHelper {

    private final JwtUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final AppLogger appLogger;
    private final ClienteRepository clienteRepository;

    public void authenticate(String email, HttpServletResponse response) {

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(email);

        String token = jwtUtil.generateToken(email);

        jwtUtil.addTokenCookie(response, token);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        Cliente cliente = clienteRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Cliente não encontrado"));

        appLogger.loginSuccess(email);

        appLogger.success(
                AcaoAuditoria.LOGIN_SUCESSO,
                cliente.getId(),
                cliente.getEmail(),
                "Autenticado via JWT");
    }

    public void addTokenCookie(
            HttpServletResponse response,
            String token) {

        jwtUtil.addTokenCookie(response, token);
    }
}