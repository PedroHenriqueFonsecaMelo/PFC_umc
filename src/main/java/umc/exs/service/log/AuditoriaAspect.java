package umc.exs.service.log;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.cliente.ClienteService;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditoriaAspect {

    private final AppLogger appLogger;
    private final ClienteService clienteService;

    @Around("@annotation(auditar)")
    public Object auditar(ProceedingJoinPoint joinPoint, Auditar auditar) throws Throwable {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String email = (auth != null && auth.isAuthenticated()) ? auth.getName() : null;

        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Long userId = null;
        String tipoUsuario = isAdmin ? "ADMIN" : "CLIENTE";

        if (!isAdmin && email != null) {
            try {
                Cliente cliente = clienteService.buscarEntidadePorEmail(email);
                if (cliente != null) {
                    userId = cliente.getId();
                }
            } catch (Exception e) {
                // Log de erro, mas continua para não bloquear a ação principal
                appLogger.error(
                        auditar.value(),
                        null,
                        "CLIENTE:" + email,
                        "Erro ao buscar cliente para auditoria: " + e.getMessage());
            }
        }

        Object result;

        try {
            result = joinPoint.proceed();

            appLogger.success(
                    auditar.value(),
                    userId,
                    email != null ? tipoUsuario + ":" + email : null,
                    "Execução com sucesso");

            return result;

        } catch (Throwable ex) {

            appLogger.error(
                    auditar.value(),
                    userId,
                    email != null ? tipoUsuario + ":" + email : null,
                    ex.getMessage());

            throw ex;
        }
    }
}