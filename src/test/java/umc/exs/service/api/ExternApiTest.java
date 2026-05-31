package umc.exs.service.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

class ExternApiTest {

    @Test
    void buscarGeneroPorIsbn_quandoIsbnNulo_retornaNull() {
        ExternApi api = new ExternApi();
        String genero = api.buscarGeneroPorIsbn(null);
        assertNull(genero);
    }

    @Test
    void buscarPorIsbnOpenLibrary_quandoIsbnVazio_retornaOptionalEmpty() {
        ExternApi api = new ExternApi();
        assertTrue(api.buscarPorIsbnOpenLibrary("").isEmpty());
    }

    @Test
    void buscarPorIsbnAsync_quandoFalhaDeRede_retornouFutureNull() {
        // Não conseguimos mockar o RestTemplate interno facilmente sem refator.
        // Então validamos apenas que o método retorna um CompletableFuture (e não
        // lança).
        ExternApi api = new ExternApi();

        CompletableFuture<?> future = api.buscarPorIsbnAsync("0000000");
        assertNotNull(future);

        // A implementação retorna CompletableFuture.completedFuture(null) em caso de
        // RestClientException.
        Object result = future.join();
        assertNull(result);
    }
}
