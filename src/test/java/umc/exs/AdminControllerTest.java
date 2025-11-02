package umc.exs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import umc.exs.controller.AdminController;
import umc.exs.model.DTO.admin.AdminDTO;
import umc.exs.model.DTO.admin.ProdutoDTO;
import umc.exs.model.DTO.admin.TrocaDTO;
import umc.exs.model.compras.Troca;
import umc.exs.model.foundation.Administrador;
import umc.exs.model.foundation.Produto;
import umc.exs.repository.AdminRepository;
import umc.exs.repository.ProdutoRepository;
import umc.exs.repository.TrocaRepository;

public class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private TrocaRepository trocaRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private AdminRepository adminRepository;

    private Long trocaId;
    private Troca mockTroca;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        trocaId = 1L;

        mockTroca = new Troca();
        mockTroca.setId(trocaId);
        mockTroca.setStatus("PENDING");
        mockTroca.setClienteId(10L);
        mockTroca.setValor(50.0);
    }

    @Test
    public void testApproveExchange_Success() {
        when(trocaRepository.findById(trocaId)).thenReturn(Optional.of(mockTroca));

        ResponseEntity<Map<String, Object>> response = adminController.approveExchange(trocaId);

        assertThat(response.getStatusCode().value(), equalTo(200));

        Map<String, Object> body = response.getBody();
        assertNotNull(body, "Response body should not be null");

        if (body != null) {
            assertThat(body.get("troca"), notNullValue());
            assertThat(body.get("cupom"), instanceOf(String.class));
        }
    }

    @Test
    public void testApproveExchange_NotFound() {
        when(trocaRepository.findById(trocaId)).thenReturn(Optional.empty());

        ResponseEntity<Map<String, Object>> response = adminController.approveExchange(trocaId);

        assertThat(response.getStatusCode().value(), equalTo(404));
    }

    @Test
    public void testApproveExchange_AlreadyApproved() {
        mockTroca.setStatus("APPROVED");
        when(trocaRepository.findById(trocaId)).thenReturn(Optional.of(mockTroca));

        ResponseEntity<Map<String, Object>> response = adminController.approveExchange(trocaId);

        assertThat(response.getStatusCode().value(), equalTo(400));
    }

    @Test
    public void testRejectExchange_Success() {
        when(trocaRepository.findById(trocaId)).thenReturn(Optional.of(mockTroca));

        ResponseEntity<TrocaDTO> response = adminController.rejectExchange(trocaId, "produto errado");

        assertThat(response.getStatusCode().value(), equalTo(200));

        TrocaDTO body = response.getBody();
        assertNotNull(body, "Response body should not be null");

        if (body != null) {
            assertThat(body.getStatus(), equalTo("REJECTED"));
            assertThat(body.getMotivoRejeicao(), equalTo("produto errado"));
        }
    }

    @Test
    public void testRejectExchange_NotFound() {
        when(trocaRepository.findById(trocaId)).thenReturn(Optional.empty());

        ResponseEntity<TrocaDTO> response = adminController.rejectExchange(trocaId, "motivo");

        assertThat(response.getStatusCode().value(), equalTo(404));
    }

    @Test
    public void testCreateAdmin_Success() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        admin.setNome("Admin Test");
        admin.setEmail("admin@test.com");

        when(adminRepository.save(any(Administrador.class))).thenReturn(admin);

        AdminDTO dto = new AdminDTO();
        dto.setNome("Admin Test");
        dto.setEmail("admin@test.com");
        dto.setPassword("123");

        ResponseEntity<AdminDTO> response = adminController.createAdmin(dto);

        assertNotNull(response.getBody(), "Response body should not be null");
        AdminDTO body = response.getBody();
        if (body != null) {
            assertThat(body.getNome(), equalTo("Admin Test"));
        }
    }

    @Test
    public void testCreateProduct_Success() {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setTitulo("Produto Teste");
        produto.setPrecificacao(100.0f);
        produto.setDescricaoDoProduto("Descrição do produto teste");


        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);

        ProdutoDTO dto = new ProdutoDTO();
        dto.setTitulo("Produto Teste");
        dto.setPrecificacao(100.0f);
        dto.setDescricaoDoProduto("Descrição do produto teste");

        ResponseEntity<ProdutoDTO> response = adminController.createProduct(dto);

        assertNotNull(response.getBody(), "Response body should not be null");
        ProdutoDTO body = response.getBody();
        if (body != null) {
            assertThat(body.getTitulo(), equalTo("Produto Teste"));
        }
    }

    @Test
    public void testDeleteProduct_Success() {
        when(produtoRepository.existsById(1L)).thenReturn(true);

        ResponseEntity<Void> response = adminController.deleteProduct(1L);

        assertThat(response.getStatusCode().value(), equalTo(204));
    }

    @Test
    public void testDeleteProduct_NotFound() {
        when(produtoRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<Void> response = adminController.deleteProduct(1L);

        assertThat(response.getStatusCode().value(), equalTo(404));
    }

    @Test
    public void testListAdmins_Success() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        admin.setNome("Admin Test");
        admin.setEmail("admin@test.com");

        when(adminRepository.findAll()).thenReturn(List.of(admin));

        ResponseEntity<List<AdminDTO>> response = adminController.listAdmins();

        assertNotNull(response.getBody(), "Response body should not be null");
        List<AdminDTO> body = response.getBody();
        if (body != null) {
            assertThat(body.size(), equalTo(1));
        }
    }

    @Test
    public void testGetAdmin_Success() {
        Administrador admin = new Administrador();
        admin.setId(1L);
        admin.setNome("Admin Test");
        admin.setEmail("admin@test.com");

        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));

        ResponseEntity<AdminDTO> response = adminController.getAdmin(1L);

        assertNotNull(response.getBody(), "Response body should not be null");
        AdminDTO body = response.getBody();
        if (body != null) {
            assertThat(body.getNome(), equalTo("Admin Test"));
        }
    }

    @Test
    public void testGetAdmin_NotFound() {
        when(adminRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseEntity<AdminDTO> response = adminController.getAdmin(1L);

        assertThat(response.getStatusCode().value(), equalTo(404));
    }
}
