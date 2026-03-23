package umc.exs.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityDebugController {

/**
 * Endpoint debug autenticação SecurityContext.
 * Retorna "no-auth" ou "user:email" se logado.
 * Útil para teste frontend JWT.
 * Não para produção.
 */
@GetMapping("/debug")
    public ResponseEntity<String> debugAuth() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("no-auth");
        }
        return ResponseEntity.ok("user:" + auth.getName());
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller debug segurança autenticação.
 * Endpoint /debug retorna status auth ou user email.
 * Para desenvolvimento/teste JWT/SecurityContext.
 * Remover em produção.
 */

