package umc.exs.service.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import umc.exs.dto.extern.GoogleBookData;
import umc.exs.dto.extern.OpenLibraryData;
import umc.exs.model.entidades.livro.Livro;

// Mudamos para @ExtendWith(MockitoExtension.class) para ser um teste unitário puro e ultra rápido
@ExtendWith(MockitoExtension.class)
class ExternApiTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExternApi api;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(api, "restTemplate", restTemplate);
    }

    // ==========================================
    // TESTES ASSÍNCRONOS (GOOGLE BOOKS)
    // ==========================================

    @Test
    void buscarPorIsbnAsync_quandoFalhaDeRede_retornouFutureNull() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBookData.class)))
                .thenThrow(new RestClientException("Simulado 429 - Google Books fora"));

        CompletableFuture<GoogleBookData> future = api.buscarPorIsbnAsync("0000000");
        
        assertNotNull(future);
        GoogleBookData result = future.join();
        assertNull(result);
    }

    // ==========================================
    // TESTES DE GÊNERO (GOOGLE BOOKS)
    // ==========================================

    @Test
    void buscarGeneroPorIsbn_quandoIsbnNulo_retornaNull() {
        String genero = api.buscarGeneroPorIsbn(null);
        assertNull(genero);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void buscarGeneroPorIsbn_quandoFalhaDeRede_retornaNull() {
        when(restTemplate.getForObject(anyString(), eq(GoogleBookData.class)))
                .thenThrow(new RestClientException("Erro de rede simulado"));

        String genero = api.buscarGeneroPorIsbn("123456789");
        assertNull(genero);
    }

    // ==========================================
    // TESTES OPEN LIBRARY
    // ==========================================

    @Test
    void buscarPorIsbnOpenLibrary_quandoFalhaDeRede_retornaOptionalEmpty() {
        // Agora o Mockito vai interceptar a chamada 100% das vezes
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
            .thenThrow(new RestClientException("Simulado 500 - OpenLibrary fora"));

        Optional<Livro> resultado = api.buscarPorIsbnOpenLibrary("1111111");
        
        assertTrue(resultado.isEmpty()); // Agora vai passar com sucesso!
    }

    @Test
    void buscarPorIsbnOpenLibrary_quandoRetornoVazio_retornaOptionalEmpty() {
        ResponseEntity<Map<String, OpenLibraryData>> responseEntity = ResponseEntity.ok(new HashMap<>());
        
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)))
            .thenReturn(responseEntity);

        Optional<Livro> resultado = api.buscarPorIsbnOpenLibrary("2222222");
        
        assertTrue(resultado.isEmpty());
    }
}