package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import umc.exs.controller.api.interaction.RankingController;
import umc.exs.dto.response.gamificacao.RankingPublicResponse;
import umc.exs.service.gamificacao.GamificacaoService;

class RankingControllerUnitTest {

    private GamificacaoService gamificacaoService;
    private RankingController controller;

    @BeforeEach
    void setUp() {
        gamificacaoService = mock(GamificacaoService.class);
        controller = new RankingController(gamificacaoService);
    }

    @Test
    void top_ParametrosDefault_PassaParaService() {
        List<RankingPublicResponse> ranking = List.of(mock(RankingPublicResponse.class));
        when(gamificacaoService.obterRankingPublico(100, "todos")).thenReturn(ranking);

        ResponseEntity<List<RankingPublicResponse>> resp = controller.top(100, "todos");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(ranking, resp.getBody());
        verify(gamificacaoService).obterRankingPublico(100, "todos");
    }

    @Test
    void top_RetornaListaMesmoComParametrosCustom() {
        List<RankingPublicResponse> ranking = List.of(mock(RankingPublicResponse.class),
                mock(RankingPublicResponse.class));
        when(gamificacaoService.obterRankingPublico(10, "mes")).thenReturn(ranking);

        ResponseEntity<List<RankingPublicResponse>> resp = controller.top(10, "mes");

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(2, resp.getBody().size());
        verify(gamificacaoService).obterRankingPublico(10, "mes");
    }
}
