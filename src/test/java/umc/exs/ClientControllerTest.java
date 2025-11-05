package umc.exs;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import umc.exs.backstage.security.JwtUtil;
import umc.exs.controller.testes.ClientControllerTestes;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.entidades.usuario.Cliente;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ClientControllerTest {

    private MockMvc mvc;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private ClientControllerTestes clientController;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(clientController).build();
        mapper = new ObjectMapper();
    }

    // ---------------------------------------------------------
    // 1️⃣ TESTE: Buscar cliente existente
    // ---------------------------------------------------------
    @Test
    void testBuscarClienteExistente() throws Exception {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Cliente Teste");
        c.setEmail("teste@example.com");
        c.setDatanasc("2000-01-01");
        c.setGen("M");
        c.setSenha("123");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));

        mvc.perform(get("/clientestestes/1")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("teste@example.com"));
    }

    // ---------------------------------------------------------
    // 2️⃣ TESTE: Buscar cliente inexistente
    // ---------------------------------------------------------
    @Test
    void testBuscarClienteInexistente() throws Exception {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/clientestestes/99")
                .header("Authorization", "Bearer fake-token")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // 3️⃣ TESTE: Criar cliente com dados válidos
    // ---------------------------------------------------------
    @Test
    void testCriarClienteValido() throws Exception {
        Map<String, Object> json = new HashMap<>();
        json.put("nome", "Cliente Novo");
        json.put("email", "novo@example.com");
        json.put("senha", "123456");
        json.put("datanasc", "2001-05-10");
        json.put("gen", "F");

        Cliente saved = new Cliente();
        saved.setId(10L);
        saved.setNome("Cliente Novo");
        saved.setEmail("novo@example.com");

        when(passwordEncoder.encode("123456")).thenReturn("encodedPass");
        when(clienteRepository.save(any(Cliente.class))).thenReturn(saved);
        when(jwtUtil.generateToken("novo@example.com")).thenReturn("fake-jwt-token");

        mvc.perform(post("/clientestestes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(json)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("fake-jwt-token"))
            .andExpect(jsonPath("$.cliente.email").value("novo@example.com"))
            .andExpect(jsonPath("$.cliente.nome").value("Cliente Novo"));
    }

    // ---------------------------------------------------------
    // 4️⃣ TESTE: Criar cliente com campos obrigatórios ausentes
    // ---------------------------------------------------------
    @Test
    void testCriarClienteCamposInvalidos() throws Exception {
        Map<String, Object> json = new HashMap<>();
        json.put("email", "invalido@example.com");
        // faltando nome e senha

        mvc.perform(post("/clientestestes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(json)))
            .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------
    // 5️⃣ TESTE: Atualizar cliente existente
    // ---------------------------------------------------------
    @Test
    void testAtualizarCliente() throws Exception {
        Cliente existente = new Cliente();
        existente.setId(1L);
        existente.setNome("Antigo");
        existente.setEmail("antigo@example.com");
        existente.setDatanasc("1999-01-01");
        existente.setGen("M");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenReturn(existente);

        ClienteDTO dto = new ClienteDTO();
        dto.setNome("Atualizado");
        dto.setEmail("novoemail@example.com");
        dto.setDatanasc("1990-02-02");
        dto.setGen("F");

        mvc.perform(put("/clientestestes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("novoemail@example.com"))
            .andExpect(jsonPath("$.nome").value("Atualizado"));
    }

    // ---------------------------------------------------------
    // 6️⃣ TESTE: Atualizar cliente inexistente
    // ---------------------------------------------------------
    @Test
    void testAtualizarClienteInexistente() throws Exception {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        ClienteDTO dto = new ClienteDTO();
        dto.setNome("Inexistente");
        dto.setEmail("x@example.com");

        mvc.perform(put("/clientestestes/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------
    // 7️⃣ TESTE: Deletar cliente inexistente
    // ---------------------------------------------------------
    @Test
    void testDeletarClienteInexistente() throws Exception {
        when(clienteRepository.findById(100L)).thenReturn(Optional.empty());

        mvc.perform(delete("/clientestestes/100")
                .header("Authorization", "Bearer token"))
            .andExpect(status().isNotFound());
    }
}
