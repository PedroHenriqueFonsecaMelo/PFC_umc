package umc.exs.handler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;
    private RedirectAttributes redirectAttributes;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    void deveRetornarBadRequestParaBusinessExceptionEmRota_API() {
        when(request.getRequestURI()).thenReturn("/api/livros/comprar");
        BusinessException ex = new BusinessException("Saldo insuficiente");

        Object result = handler.handleBusinessException(ex, request, redirectAttributes);

        assertInstanceOf(ResponseEntity.class, result);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Saldo insuficiente", body.get("error"));
    }

    @Test
    void deveRedirecionarParaBusinessExceptionEmRotaMVC() {
        when(request.getRequestURI()).thenReturn("/livros/checkout");
        when(request.getHeader("Referer")).thenReturn(null);
        BusinessException ex = new BusinessException("Erro de negócio");

        Object result = handler.handleBusinessException(ex, request, redirectAttributes);

        assertInstanceOf(String.class, result);
        assertEquals("redirect:/", result);
    }

    @Test
    void deveRetornarErroInternoParaExcecaoGenericaEmRota_API() {
        when(request.getRequestURI()).thenReturn("/api/clientes/perfil");
        Exception ex = new RuntimeException("Erro inesperado");

        Object result = handler.handleGenericException(request, ex);

        assertInstanceOf(ResponseEntity.class, result);
        ResponseEntity<?> response = (ResponseEntity<?>) result;
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    void deveRetornarPagina500ParaExcecaoGenericaEmRotaMVC() {
        when(request.getRequestURI()).thenReturn("/livros/vitrine");
        Exception ex = new RuntimeException("Erro inesperado");

        Object result = handler.handleGenericException(request, ex);

        assertInstanceOf(org.springframework.web.servlet.ModelAndView.class, result);
        org.springframework.web.servlet.ModelAndView mav =
            (org.springframework.web.servlet.ModelAndView) result;
        assertEquals("error/500", mav.getViewName());
    }

    @Test
    void deveRetornar404ParaNoResourceFoundException() {
        org.springframework.web.servlet.resource.NoResourceFoundException ex =
            new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/pagina-inexistente");

        ResponseEntity<Object> response = handler.handleNoResourceFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
