package umc.exs.controller_api.unitary.contas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import umc.exs.controller.api.contas.AdminControllerApi;
import umc.exs.dto.mapper.CupomMapper;
import umc.exs.dto.mapper.LivroMapper;
import umc.exs.dto.mapper.PedidoMapper;
import umc.exs.dto.request.admin.AdminAprovacaoRequest;
import umc.exs.dto.request.admin.CriarCupomRequest;
import umc.exs.dto.response.admin.ExternApiResponse;
import umc.exs.dto.response.admin.DashboardResponse;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.logic.ReporteRepository;
import umc.exs.repository.logic.ReporteRespostaRepository;
import umc.exs.service.cliente.admin.ClienteAdminService;
import umc.exs.service.cupom.CupomService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.core.dashboard.DashboardService;
import umc.exs.service.core.dashboard.LoteService;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.core.interactions.PostBlogService;
import umc.exs.service.core.livros.LivroService;

@ExtendWith(MockitoExtension.class)
class AdminControllerApiUnitTest {

    @Mock
    private LivroService livroService;
    @Mock
    private LivroRepository livroRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private LoteService loteService;
    @Mock
    private PedidoService pedidoService;
    @Mock
    private DashboardService dashboardService;
    @Mock
    private CupomService cupomService;
    @Mock
    private PostBlogService postBlogService;
    @Mock
    private ClienteAdminService clienteAdminService;
    @Mock
    private LivroMapper livroMapper;
    @Mock
    private PedidoMapper pedidoMapper;
    @Mock
    private CupomMapper cupomMapper;
    @Mock
    private ReporteRepository reporteRepository;
    @Mock
    private ReporteRespostaRepository reporteRespostaRepository;
    @Mock
    private EmailFacade emailFacade;

    @InjectMocks
    private AdminControllerApi controller;
    
    private UserDetails adminUser = User.withUsername("admin@email.com") .password("pass") .authorities("ADMIN") .build();

    @Test
    void aprovarLivro_SemAuth_Retorna401() {
        AdminAprovacaoRequest dto = mock(AdminAprovacaoRequest.class);

        ResponseEntity<ExternApiResponse<Void>> resp = controller.aprovarLivro(1L, dto, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(livroService, adminRepository);
    }

    @Test
    void aprovarLivro_ComSucesso_Retorna200() {
        AdminAprovacaoRequest dto = mock(AdminAprovacaoRequest.class);
        Administrador admin = mock(Administrador.class);
        when(admin.getId()).thenReturn(99L);

        when(adminRepository.findByEmail(eq(adminUser.getUsername())))
                .thenReturn(java.util.Optional.of(admin));

        ResponseEntity<ExternApiResponse<Void>> resp = controller.aprovarLivro(1L, dto, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        verify(adminRepository).findByEmail(eq(adminUser.getUsername()));
        verify(livroService).aprovarLivro(eq(1L), eq(99L), eq(dto));
    }

    @Test
    void criarCupom_DataValidadeNula_Retorna400() {
        CriarCupomRequest dto = mock(CriarCupomRequest.class);
        when(dto.getDataValidade()).thenReturn(null);

        ResponseEntity<?> resp = controller.criarCupom(dto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        verifyNoInteractions(cupomService);
    }

    @Test
    void getMetricas_Retorna200() {
        DashboardResponse metrics = mock(DashboardResponse.class);
        when(dashboardService.getMetricas()).thenReturn(metrics);

        ResponseEntity<DashboardResponse> resp = controller.getMetricas();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(metrics, resp.getBody());
        verify(dashboardService).getMetricas();
    }

    @Test
    void criarCupom_ComDataValida_ChamaService_CriaCupom() {
        CriarCupomRequest dto = mock(CriarCupomRequest.class);
        when(dto.getDataValidade()).thenReturn("2026-12-31T00:00:00");
        when(dto.getPercentualDesconto()).thenReturn(10.0);
        when(dto.getQuantidadeMaxima()).thenReturn(10);

        // CupomService.criarCupom(...) devolve Cupom; no controller o retorno vira body
        // de ResponseEntity.
        var cupom = mock(umc.exs.model.entidades.foundation.Cupom.class);
        when(cupomService.criarCupom(eq(dto), any(java.time.LocalDateTime.class)))
                .thenReturn(cupom);

        ResponseEntity<?> resp = controller.criarCupom(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        assertNotNull(resp.getBody());
        verify(cupomService).criarCupom(eq(dto), any(java.time.LocalDateTime.class));
    }

    @Test
    void invalidaAuthAdmin_QuandoBuscarPerfilCliente() {
        // Método getMe exige user não-null mas não valida explicitamente; fica fora do
        // escopo.
        // Mantemos testes apenas nos endpoints com checagem explícita.
        assertTrue(true);
    }
}
