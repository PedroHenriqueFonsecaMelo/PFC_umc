package umc.exs.controller.api.control;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint de diagnóstico — RESTRITO A ADMIN.
 * ⚠️ Remover ou desabilitar completamente em produção.
 */
@Slf4j
@RestController
public class SecurityDebugController {

    @GetMapping("/debug")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> debugAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("no-auth");
        }
        log.warn("DEBUG endpoint acessado por: {}", auth.getName());
        return ResponseEntity.ok("user:" + auth.getName());
    }
}
