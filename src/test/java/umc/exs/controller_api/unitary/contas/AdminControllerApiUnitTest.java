package umc.exs.controller_api.unitary.contas;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

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
import umc.exs.dto.request.livro.RejeicaoLivroRequest;
import umc.exs.dto.response.admin.ExternApiResponse;
import umc.exs.dto.response.admin.DashboardResponse;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote.LoteStatus;
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
class AdminControllerApiTest {

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

    private UserDetails adminUser;

    @BeforeEach
    void setup() {
        adminUser = User.withUsername("admin@email.com")
                .password("123")
                .authorities("ADMIN")
                .build();
    }

    @Test
    void aprovarLivro_semAuth_retorna401() {
        AdminAprovacaoRequest dto = new AdminAprovacaoRequest();

        ResponseEntity<ExternApiResponse<Void>> resp = controller.aprovarLivro(1L, dto, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
        verifyNoInteractions(livroService);
    }

    @Test
    void aprovarLivro_sucesso() {
        AdminAprovacaoRequest dto = new AdminAprovacaoRequest();
        Administrador admin = new Administrador();
        admin.setId(10L);

        when(adminRepository.findByEmail(adminUser.getUsername()))
                .thenReturn(Optional.of(admin));

        ResponseEntity<ExternApiResponse<Void>> resp = controller.aprovarLivro(1L, dto, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(adminRepository).findByEmail(adminUser.getUsername());
        verify(livroService).aprovarLivro(1L, 10L, dto);
    }

    @Test
    void rejeitarLivro_semAuth() {
        RejeicaoLivroRequest dto = new RejeicaoLivroRequest();

        ResponseEntity<?> resp = controller.rejeitarLivro(1L, dto, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void rejeitarLivro_sucesso() {
        RejeicaoLivroRequest dto = new RejeicaoLivroRequest();
        dto.setComentario("Ruim");
        dto.setEstado("DANIFICADO");

        Administrador admin = new Administrador();
        admin.setId(5L);

        when(adminRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(admin));

        ResponseEntity<?> resp = controller.rejeitarLivro(1L, dto, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(livroService)
                .rejeitarLivro(1L, 5L, "DANIFICADO", "Ruim");
    }

    @Test
    void criarCupom_dataNula() {
        CriarCupomRequest dto = new CriarCupomRequest();

        ResponseEntity<?> resp = controller.criarCupom(dto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        verifyNoInteractions(cupomService);
    }

    @Test
    void criarCupom_dataInvalida() {
        CriarCupomRequest dto = new CriarCupomRequest();
        dto.setDataValidade("data_errada");

        ResponseEntity<?> resp = controller.criarCupom(dto);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void criarCupom_sucesso() {
        CriarCupomRequest dto = new CriarCupomRequest();
        dto.setDataValidade("2026-12-31T00:00:00");

        Cupom cupom = new Cupom();

        when(cupomService.criarCupom(any(), any(LocalDateTime.class)))
                .thenReturn(cupom);

        ResponseEntity<?> resp = controller.criarCupom(dto);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(cupomService)
                .criarCupom(eq(dto), any(LocalDateTime.class));
    }

    @Test
    void getMetricas() {
        DashboardResponse mock = new DashboardResponse();

        when(dashboardService.getMetricas()).thenReturn(mock);

        ResponseEntity<DashboardResponse> resp = controller.getMetricas();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals(mock, resp.getBody());
    }

    @Test
    void getMe() {
        Administrador admin = new Administrador();
        admin.setNome("Admin Teste");

        when(adminRepository.findByEmail(adminUser.getUsername()))
                .thenReturn(Optional.of(admin));

        ResponseEntity<?> resp = controller.getMe(adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void getPerfilCliente_notFound() {
        when(clienteAdminService.getPerfilCliente(1L))
                .thenThrow(new IllegalArgumentException("Erro"));

        ResponseEntity<?> resp = controller.getPerfilCliente(1L);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void contarReportesNaoLidos() {
        when(reporteRepository.countByLidoFalse()).thenReturn(5L);

        ResponseEntity<?> resp = controller.contarReportesNaoLidos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void marcarReporteLido_notFound() {
        when(reporteRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.marcarReporteLido(1L);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // =============================
    // LOTES
    // =============================

    @Test
    void listarLotesPendentes() {
        when(loteService.listarPendentesComCliente()).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarLotesPendentes();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void detalharLote() {
        var lote = mock(umc.exs.model.entidades.foundation.Lote.class);
        when(lote.getId()).thenReturn(1L);
        when(lote.getCodigoProtocolo()).thenReturn("ABC");
        when(lote.getStatus()).thenReturn(LoteStatus.PENDENTE);
        when(lote.getDataCriacao()).thenReturn(LocalDateTime.now());
        when(lote.getCliente()).thenReturn(null);

        when(loteService.findByIdComCliente(1L)).thenReturn(lote);
        when(livroService.listarLivrosPorLote(1L)).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.detalharLote(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void listarLivrosLote() {
        when(livroService.listarLivrosPorLote(1L)).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarLivrosLote(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // =============================
    // LIVROS ADMIN
    // =============================

    @Test
    void adicionarLivro_semAuth() {
        var req = new umc.exs.dto.request.admin.LivroAdminRequest();

        ResponseEntity<?> resp = controller.adicionarLivro(req, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void adicionarLivro_sucesso() {
        var req = new umc.exs.dto.request.admin.LivroAdminRequest();
        var admin = new Administrador();
        admin.setId(1L);

        when(adminRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(admin));

        ResponseEntity<?> resp = controller.adicionarLivro(req, adminUser);

        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        verify(livroService).adicionarLivroAdmin(req);
    }

    @Test
    void editarLivro_semAuth() {
        var req = new umc.exs.dto.request.admin.LivroAdminRequest();

        ResponseEntity<?> resp = controller.editarLivro(1L, req, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    @Test
    void deletarLivro_semAuth() {
        ResponseEntity<?> resp = controller.deletarLivro(1L, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

    // =============================
    // PEDIDOS
    // =============================

    @Test
    void listarPedidos() {
        when(pedidoService.listarTodos()).thenReturn(java.util.List.of());
        when(pedidoMapper.toResponseList(any())).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarPedidos();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void atualizarEnvio_semAuth() {
        var dto = new umc.exs.dto.request.admin.AtualizarEnvioRequest();

        assertThrows(Exception.class,
                () -> controller.atualizarEnvio(1L, dto, null));
    }

    // =============================
    // CUPOM DELETE
    // =============================

    @Test
    void invalidarCupom() {
        ResponseEntity<?> resp = controller.invalidarCupom(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(cupomService).invalidarCupom(1L);
    }

    // =============================
    // CLIENTES
    // =============================

    @Test
    void listarClientes() {
        when(clienteAdminService.listarClientes()).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarClientesList();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void suspenderCliente() {
        var req = new umc.exs.dto.request.admin.SuspenderClienteRequest();
        req.setMotivo("teste");
        req.setDiasSuspensao(1);
        req.setNotificarEmail(true);

        ResponseEntity<?> resp = controller.suspenderCliente(1L, req, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(clienteAdminService)
                .suspenderCliente(eq(1L), anyString(), anyInt(), anyBoolean(), anyString());
    }

    @Test
    void removerCliente() {
        var req = new umc.exs.dto.request.admin.RemoverClienteRequest();
        req.setMotivo("teste");

        ResponseEntity<?> resp = controller.removerCliente(1L, req, adminUser);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(clienteAdminService)
                .removerCliente(eq(1L), anyString(), anyBoolean(), anyString());
    }

    @Test
    void reativarCliente() {
        ResponseEntity<?> resp = controller.reativarCliente(1L, null);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(clienteAdminService)
                .reativarCliente(eq(1L), isNull(), eq(false));
    }

    // =============================
    // REPORTES
    // =============================

    @Test
    void listarReportes() {
        when(reporteRepository.findAllByOrderByDataCriacaoDesc())
                .thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarReportes();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    @Test
    void responderReporte_notFound() {
        when(reporteRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.responderReporte(1L, java.util.Map.of());

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    @Test
    void excluirReporte_notFound() {
        when(reporteRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<?> resp = controller.excluirReporte(1L);

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
    }

    // =============================
    // BLOG (faltava)
    // =============================

    @Test
    void listarPostsBlog() {
        when(postBlogService.listarTodos()).thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.listarPostsBlog();

        assertEquals(HttpStatus.OK, resp.getStatusCode());
    }

    // =============================
    // REPORTES (faltava sucesso)
    // =============================

    @Test
    void responderReporte_sucesso() {
        var reporte = mock(umc.exs.model.entidades.logic.Reporte.class);

        when(reporteRepository.findById(1L))
                .thenReturn(Optional.of(reporte));

        ResponseEntity<?> resp = controller.responderReporte(1L, java.util.Map.of("mensagem", "ok"));

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(reporteRespostaRepository).save(any());
    }

    @Test
    void excluirReporte_sucesso() {
        when(reporteRepository.existsById(1L)).thenReturn(true);
        when(reporteRespostaRepository.findByReporteIdOrderByDataEnvioAsc(1L))
                .thenReturn(java.util.List.of());

        ResponseEntity<?> resp = controller.excluirReporte(1L);

        assertEquals(HttpStatus.OK, resp.getStatusCode());

        verify(reporteRepository).deleteById(1L);
    }

    // =============================
    // UPLOAD FOTO (faltava)
    // =============================

    @Test
    void uploadFotoLivro_semAuth() {
        var file = mock(org.springframework.web.multipart.MultipartFile.class);

        ResponseEntity<?> resp = controller.uploadFotoLivro(file, null);

        assertEquals(HttpStatus.UNAUTHORIZED, resp.getStatusCode());
    }

}