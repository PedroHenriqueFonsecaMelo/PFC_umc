package umc.exs.controller_api.unitary.interaction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.interaction.GamificacaoController;
import umc.exs.dto.response.gamificacao.MeuPerfilGameResponse;
import umc.exs.dto.response.gamificacao.RankingDetalhadoResponse;
import umc.exs.service.gamificacao.GamificacaoService;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

class GamificacaoControllerUnitTest {

    private GamificacaoService gamificacaoService;
    private GamificacaoController controller;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        gamificacaoService = mock(GamificacaoService.class);
        controller = new GamificacaoController(gamificacaoService);
        user = User.withUsername("user@email.com")
                .password("pass")
                .authorities(new SimpleGrantedAuthority("USER"))
                .build();
    }

    @Test
    void obterRanking_Sucesso_RetornaOk() {
        List<RankingDetalhadoResponse> ranking = List.of(mock(RankingDetalhadoResponse.class));
        when(gamificacaoService.obterRankingTop5()).thenReturn(ranking);

        ResponseEntity<List<RankingDetalhadoResponse>> resp = controller.obterRanking();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(ranking, resp.getBody());
        verify(gamificacaoService).obterRankingTop5();
    }

    @Test
    void obterMeuPerfil_SemAuth_Retorna401() {
        ResponseEntity<?> resp = controller.obterMeuPerfil(null);
        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        assertEquals("Usuário não autenticado.", resp.getBody());
        verifyNoInteractions(gamificacaoService);
    }

    @Test
    void obterMeuPerfil_ComAuth_RetornaOk() {
        MeuPerfilGameResponse perfil = mock(MeuPerfilGameResponse.class);
        when(gamificacaoService.obterMeuPerfil("user@email.com")).thenReturn(perfil);

        ResponseEntity<?> resp = controller.obterMeuPerfil(user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(perfil, resp.getBody());
        verify(gamificacaoService).obterMeuPerfil("user@email.com");
    }
}
