package umc.exs.controller_web.unitary;

import java.util.Optional;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.controller.web.ClientController;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ClientControllerUnitTest {

    private ClienteService clienteService;
    private AuthHelper authHelper;
    private JwtUtil jwtUtil;
    private ClientController controller;

    @BeforeEach
    void setUp() {
        clienteService = mock(ClienteService.class);
        authHelper = mock(AuthHelper.class);
        jwtUtil = mock(JwtUtil.class);
        controller = new ClientController(clienteService, authHelper, jwtUtil);
    }

    @Test
    void deveExibirFormularioCadastroComClienteNovo() {
        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        String view = controller.exibirFormularioCadastro(response, model);

        assertEquals("cliente/cadastro_cliente", view);
        assertTrue(model.containsAttribute("cliente"));
        verify(jwtUtil).clearJwtCookie(response);
    }

    @Test
    void deveRegistrarClienteComSucesso() {
        SignupDTO dto = new SignupDTO();
        dto.setEmail("client@example.com");
        dto.setSenha("senha123");
        dto.setConfirmPassword("senha123");

        BindingResult result = new BeanPropertyBindingResult(dto, "cliente");
        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        ClienteDTO salvo = new ClienteDTO(1L, null, "client@example.com", null, null, null, null, 0.0, null, null, null);
        when(clienteService.salvarCliente(dto)).thenReturn(salvo);

        String view = controller.registrarCliente(dto, result, "senha123", model, response);

        assertEquals("redirect:/clientes/meu-perfil", view);
        verify(authHelper).authenticateAndSetCookie("client@example.com", 1L, response, "CADASTRO_SUCESSO");
    }

    @Test
    void deveRetornarLoginQuandoCredenciaisInvalidas() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setEmail("client@example.com");
        loginDTO.setSenha("senha123");

        BindingResult result = new BeanPropertyBindingResult(loginDTO, "loginData");
        Model model = new ExtendedModelMap();
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(clienteService.autenticarCliente("client@example.com", "senha123"))
                .thenReturn(Optional.empty());

        String view = controller.realizarLogin(loginDTO, result, model, response);

        assertEquals("cliente/login_cliente", view);
        assertEquals("E-mail ou senha inválidos.", model.asMap().get("erro"));
    }
}
