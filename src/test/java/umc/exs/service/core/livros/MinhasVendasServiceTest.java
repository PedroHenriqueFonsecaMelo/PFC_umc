package umc.exs.service.core.livros;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.enums.StatusVenda;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;

@ExtendWith(MockitoExtension.class)
class MinhasVendasServiceTest {

    @Mock
    LivroRepository livroRepository;

    @Mock
    PedidoRepository pedidoRepository;

    @InjectMocks
    MinhasVendasService service;

    @Test
    void listarMinhasVendas_deveConverterLivros() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setTitulo("Livro");
        livro.setAutor("Autor");
        livro.setIsbn("123");
        livro.setFotosUrls("[\"/img.jpg\"]");
        livro.setEstadoAprovado(null);
        livro.setPrecoAprovado(50.0);
        livro.setDataAnuncio(LocalDateTime.now());

        when(livroRepository.findAllByVendedorEmail("user@test.com")).thenReturn(List.of(livro));

        List<VendaResponse.Resumo> vendas = service.listarMinhasVendas("user@test.com");

        assertEquals(1, vendas.size());
        assertEquals("Livro", vendas.get(0).getTitulo());
    }

    @Test
    void detalharMinhaVenda_quandoLivroExiste_retornaResumo() {
        Livro livro = new Livro();
        livro.setId(1L);
        livro.setTitulo("Livro");
        livro.setAutor("Autor");
        livro.setIsbn("123");
        livro.setIdioma("PT");
        livro.setResumoOficial("Resumo");
        livro.setEstadoAprovado(null);
        livro.setPrecoAprovado(50.0);
        livro.setFotosUrls("[]");
        livro.setDataAnuncio(LocalDateTime.now());
        livro.setDataAprovacao(LocalDateTime.now());
        livro.setAprovado(true);

        when(livroRepository.findByIdAndVendedorEmail(1L, "user@test.com")).thenReturn(Optional.of(livro));

        VendaResponse response = service.detalharMinhaVenda(1L, "user@test.com");

        assertEquals(1L, response.getId());
        assertEquals(StatusVenda.NA_VITRINE, response.getStatusVenda());
    }
}
