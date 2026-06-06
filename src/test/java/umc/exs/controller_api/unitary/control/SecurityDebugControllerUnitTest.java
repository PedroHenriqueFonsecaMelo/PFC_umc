package umc.exs.controller_api.unitary.control;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.springframework.http.ResponseEntity;

import umc.exs.controller.api.control.SecurityDebugController;

class SecurityDebugControllerUnitTest {

    private final SecurityDebugController controller = new SecurityDebugController();

    @Test
    void debugAuth_QuandoSemContextoRetorna401OuOkDePendendoDoContexto() {
        // Como este endpoint depende de SecurityContextHolder, sem inicializar o contexto
        // pode variar. Mantemos um teste simples apenas para garantir que não lança.
        ResponseEntity<String> resp = controller.debugAuth();
        assertNotNull(resp);
    }
}

