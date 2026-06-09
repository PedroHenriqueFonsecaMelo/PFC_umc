package umc.exs.service.core.livros.delegado;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import umc.exs.model.entidades.livro.Livro;

class LivroPromocaoServiceUnitTest {

    private final LivroPromocaoService service = new LivroPromocaoService();

    @Test
    void aplicarPromocao_quandoPromoAtiva_deveCalcularPrecoPromo() {
        Livro livro = new Livro();

        double preco = 100.0;
        double desconto = 20.0;
        LocalDateTime expira = LocalDateTime.now().plusDays(1);

        service.aplicarPromocao(livro, true, preco, desconto, expira);

        assertEquals(preco, livro.getPrecoOriginal());
        assertEquals(80.0, livro.getPrecoAprovado());
        assertEquals(expira, livro.getPromocaoExpira());
    }

    @Test
    void aplicarPromocao_quandoPromoInativa_deveResetarPrecoOriginal() {
        Livro livro = new Livro();
        livro.setPrecoOriginal(50.0);
        livro.setPrecoAprovado(50.0);
        livro.setPromocaoExpira(LocalDateTime.now().minusDays(1));

        service.aplicarPromocao(livro, false, 120.0, 10.0, null);

        assertNull(livro.getPrecoOriginal());
        assertEquals(120.0, livro.getPrecoAprovado());
        assertNull(livro.getPromocaoExpira());
    }

    @Test
    void aplicarPromocao_quandoPromoAtiva_comDescontoZero_deveManterPrecoAprovado() {
        Livro livro = new Livro();

        LocalDateTime expira = LocalDateTime.now().plusHours(10);

        service.aplicarPromocao(livro, true, 200.0, 0.0, expira);

        assertEquals(200.0, livro.getPrecoOriginal());
        assertEquals(200.0, livro.getPrecoAprovado());
        assertEquals(expira, livro.getPromocaoExpira());
    }

    @Test
    void aplicarPromocao_quandoPromoAtiva_comPercentualMaiorQue100_deveAplicarFormulaDireto() {
        Livro livro = new Livro();

        // Ex: 150% => 1 - 1.5 = -0.5 => preço negativo (apenas verifica fórmula)
        LocalDateTime expira = LocalDateTime.now().plusDays(2);
        service.aplicarPromocao(livro, true, 100.0, 150.0, expira);

        assertEquals(100.0, livro.getPrecoOriginal());
        assertEquals(-50.0, livro.getPrecoAprovado());
        assertEquals(expira, livro.getPromocaoExpira());
    }

    @Test
    void aplicarPromocao_quandoPromoAtiva_comDescontoNulo_deveCalcularComDoubleNullUnboxing() {
        Livro livro = new Livro();
        LocalDateTime expira = LocalDateTime.now().plusDays(1);

        // Sem validação no service: percentualDesconto null deve causar NullPointerException
        assertThrows(NullPointerException.class, () -> service.aplicarPromocao(livro, true, 100.0, null, expira));
    }
}

