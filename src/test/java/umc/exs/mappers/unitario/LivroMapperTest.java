package umc.exs.mappers.unitario;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import umc.exs.DTOs.livro.LivroRequestDTO;
import umc.exs.mappers.LivroMapper;
import umc.exs.model.entidades.livro.Livro;

import static org.junit.jupiter.api.Assertions.*;

class LivroMapperTest {

    private final LivroMapper livroMapper = Mappers.getMapper(LivroMapper.class);

    @Test
    void deveMapearDTOparaEntidade() {
        LivroRequestDTO dto = new LivroRequestDTO();
        dto.setTitulo("O Senhor dos Anéis");
        dto.setAutor("J.R.R. Tolkien");
        dto.setIsbn("978-3-16-148410-0");

        Livro livro = livroMapper.paraEntidade(dto);

        assertNotNull(livro);
        assertEquals(dto.getTitulo(), livro.getTitulo());
        assertEquals(dto.getAutor(), livro.getAutor());
        assertEquals(dto.getIsbn(), livro.getIsbn());
        // Campos ignorados no mapper devem estar nulos (ou default do @Builder)
        assertNull(livro.getId());
        assertNull(livro.getLote());
        assertNull(livro.getDataAnuncio());
        assertEquals(false, livro.getAprovado());
        assertNull(livro.getPrecoAprovado());
        assertNull(livro.getEstadoAprovado());
        assertNull(livro.getDataAprovacao());
        assertNull(livro.getAdminAprovadorId());
    }

    @Test
    void deveMapearEntidadeParaDTO() {
        Livro livro = Livro.builder()
                .titulo("O Hobbit")
                .autor("J.R.R. Tolkien")
                .isbn("978-0-261-10230-5")
                .build();

        LivroRequestDTO dto = livroMapper.paraDTO(livro);

        assertNotNull(dto);
        assertEquals(livro.getTitulo(), dto.getTitulo());
        assertEquals(livro.getAutor(), dto.getAutor());
        assertEquals(livro.getIsbn(), dto.getIsbn());
    }
}

