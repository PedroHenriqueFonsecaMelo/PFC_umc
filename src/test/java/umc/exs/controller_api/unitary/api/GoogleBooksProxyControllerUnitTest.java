package umc.exs.controller_api.unitary.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.client.RestTemplate;

import umc.exs.controller.api.GoogleBooksProxyController;

class GoogleBooksProxyControllerUnitTest {

    private GoogleBooksProxyController controller;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws Exception {
        controller = new GoogleBooksProxyController();
        restTemplate = mock(RestTemplate.class);

        // Troca o RestTemplate interno (campo final) via reflexão
        Field f = GoogleBooksProxyController.class.getDeclaredField("restTemplate");
        f.setAccessible(true);
        f.set(controller, restTemplate);

        Field apiKey = GoogleBooksProxyController.class.getDeclaredField("apiKey");
        apiKey.setAccessible(true);
        apiKey.set(controller, "test-key");
    }

    @Test
    void buscarPorIsbn_Sucesso_RetornaOk() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("response-json");

        ResponseEntity<String> resp = controller.buscarPorIsbn("123");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("response-json", resp.getBody());
        verify(restTemplate).getForObject(contains("isbn:123"), eq(String.class));
    }

    @Test
    void buscarPorIsbn_QuandoRestTemplateFalha_Retorna404() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("boom"));

        ResponseEntity<String> resp = controller.buscarPorIsbn("123");

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNull(resp.getBody());
    }
}
