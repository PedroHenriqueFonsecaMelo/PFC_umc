package umc.exs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import umc.exs.controller.ClientController;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;
import umc.exs.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ClientController.class)
public class ClientControllerTest {

    @Autowired
    private MockMvc mvc;

    // Use @Mock instead of @MockBean
    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private final ObjectMapper om = new ObjectMapper();

    // Inject mocks into the controller

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Initializes the mocks
    }

    // Teste de Login com credenciais válidas
    @Test
    public void testLoginWithValidCredentials() throws Exception {
        // Dados de login válidos para o teste
        String jsonRequest = "{ \"email\": \"testuser@example.com\", \"password\": \"Senha@123\" }";

        // Enviar o POST para /auth/login
        mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk()) // Espera um status 200
                .andExpect(jsonPath("$.token").exists()); // Espera o campo "token" na resposta
    }

    // Teste de Criação de Cliente com Email Inválido
    @Test
    public void testCreateClienteWithInvalidEmail() throws Exception {
        // Dados com email inválido
        String jsonRequest = "{ \"nome\": \"Teste Cliente\", \"email\": \"invalid-email\", \"datanasc\": \"1990-01-01\", \"gen\": \"M\", \"senha\": \"Senha@123\" }";

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest()) // Espera um erro 400, já que o email é inválido
                .andExpect(jsonPath("$.error").value("Invalid email format")); // Espera a mensagem de erro
    }

    // Teste de Criação de Cliente com Dados Válidos
    @Test
    public void testCreateClienteWithValidData() throws Exception {
        // Dados válidos, incluindo a aceitação dos termos e da política de privacidade
        String jsonRequest = "{ \"nome\": \"Teste Cliente\", \"email\": \"testuser@example.com\", \"datanasc\": \"1990-01-01\", \"gen\": \"M\", \"senha\": \"Senha@123\", \"termsAccepted\": true, \"privacyAccepted\": true }";

        mvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated()) // Espera status 201 Created
                .andExpect(jsonPath("$.id").exists()); // Espera o ID do cliente ser retornado
    }

    // Teste de Acesso Não Autorizado (sem autenticação)
    @Test
    public void testGetClientUnauthorized() throws Exception {
        mvc.perform(get("/client/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Espera status 401 (não autenticado)
    }

    // Teste de Acesso Autenticado (com token JWT válido)
    @Test
    public void testGetClientAuthenticated() throws Exception {
        // Gerar um token JWT válido para o teste (substitua com um token real)
        String token = "valid-jwt-token"; // Este valor precisa ser um token real ou mockado

        mvc.perform(get("/client/1")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Espera status 200 OK
                .andExpect(jsonPath("$.id").value(1)); // Espera que o cliente com ID 1 seja retornado
    }

    @Test
    public void criarCliente_acceptsPortugueseFields_returnsToken_and_clienteDTO() throws Exception {
        String email = "cli+" + System.currentTimeMillis() + "@example.com";
        Map<String, Object> payload = Map.of(
            "nome", "Cliente Teste",
            "email", email,
            "senha", "abc123",
            "datanasc", "1990-01-01",
            "gen", "M",
            "termsAccepted", true,
            "privacyAccepted", true
        );

        when(clienteRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("abc123")).thenReturn("encoded");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });
        when(jwtUtil.generateToken(anyString())).thenReturn("token-123");

        mvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.cliente.email").value(email));

        // verify mapper produced entity saved
        ArgumentCaptor<Cliente> cap = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(cap.capture());
        Cliente saved = cap.getValue();
        assert saved.getEmail().equals(email);
    }

    @Test
    public void buscarCliente_returnsClienteDTO_usingClienteMapper() throws Exception {
        Cliente c = new Cliente();
        c.setId(5L);
        c.setEmail("c5@example.com");
        c.setNome("C5");

        when(clienteRepository.findById(5L)).thenReturn(Optional.of(c));

        mvc.perform(get("/clientes/5")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(5))
            .andExpect(jsonPath("$.email").value("c5@example.com"));
    }
}
