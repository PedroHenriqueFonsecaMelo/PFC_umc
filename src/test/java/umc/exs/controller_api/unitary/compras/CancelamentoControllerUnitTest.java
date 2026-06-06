package umc.exs.controller_api.unitary.compras;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.compras.CancelamentoController;
import umc.exs.dto.request.admin.CancelamentoRequest;
import umc.exs.model.entidades.foundation.SolicitacaoCancelamento;
import umc.exs.model.enums.MotivoCategoria;
import umc.exs.service.cancelamento.CancelamentoService;

class CancelamentoControllerUnitTest {

    private CancelamentoService service;
    private CancelamentoController controller;
    private UserDetails adminUser;

    @BeforeEach
    void setUp() {
        service = mock(CancelamentoService.class);
        controller = new CancelamentoController(service);

        adminUser = User.withUsername("admin@email.com")
                .password("pass")
                .authorities("ADMIN")
                .build();
    }

    @Test
    void solicitar_ComSucesso_RetornaOk() {
        Long pedidoId = 10L;

        CancelamentoRequest req = new CancelamentoRequest();
        req.setMotivoCategoria(MotivoCategoria.DECISAO_ADMINISTRATIVA);
        req.setMotivoDescricao("cancelar pedido por motivo");

        SolicitacaoCancelamento solicitacao = mock(SolicitacaoCancelamento.class);

        when(service.solicitarCancelamento(eq(pedidoId), eq(adminUser.getUsername()), eq(req)))
                .thenReturn(solicitacao);

        ResponseEntity<?> resp = controller.solicitar(pedidoId, req, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        verify(service).solicitarCancelamento(eq(pedidoId), eq(adminUser.getUsername()), eq(req));
    }

    @Test
    void solicitar_Exception_Retorna400() {
        Long pedidoId = 10L;

        CancelamentoRequest req = new CancelamentoRequest();
        req.setMotivoCategoria(MotivoCategoria.DECISAO_ADMINISTRATIVA);
        req.setMotivoDescricao("cancelar pedido por motivo");

        when(service.solicitarCancelamento(eq(pedidoId), eq(adminUser.getUsername()), eq(req)))
                .thenThrow(new IllegalArgumentException("erro"));

        ResponseEntity<?> resp = controller.solicitar(pedidoId, req, adminUser);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("erro", ((Map<?, ?>) resp.getBody()).get("message"));
        verify(service).solicitarCancelamento(eq(pedidoId), eq(adminUser.getUsername()), eq(req));
    }

    @Test
    void cancelarPeloAdmin_JustificativaMuitoCurta_Retorna400() {
        Map<String, String> body = Map.of(
                "motivoCategoria", "DECISAO_ADMINISTRATIVA",
                "justificativa", "curto");

        ResponseEntity<?> resp = controller.cancelarPeloAdmin(1L, body);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals(
                "Justificativa obrigatória (mínimo 10 caracteres).",
                ((Map<?, ?>) resp.getBody()).get("erro"));
        verifyNoInteractions(service);
    }
}

