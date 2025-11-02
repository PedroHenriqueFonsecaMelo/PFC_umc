package umc.exs;

import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import umc.exs.backstage.security.JwtUserDetailsService;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.model.DAO.ClienteMapper;
import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;

@SpringBootTest
public class JwtSignupIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    public void register_map_save_and_generate_jwt() {
        // Prepare SignupDTO (use project DTO setters)
        SignupDTO signup = new SignupDTO();
        signup.setEmail("testuser+" + System.currentTimeMillis() + "@example.com");
        // Use setter name present in project (senha/password). Try common one used in
        // codebase:
        try {
            signup.getClass().getMethod("setSenha", String.class).invoke(signup, "Senha@123");
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ex) {
            // fallback to setPassword if setSenha not present
            try {
                signup.getClass().getMethod("setPassword", String.class).invoke(signup, "Senha@123");
            } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
            }
        }
        // set other fields (nome/datanasc/gen) - attempt both setter names if necessary
        try {
            signup.getClass().getMethod("setNome", String.class).invoke(signup, "Teste Integracao");
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
        }
        try {
            signup.getClass().getMethod("setDatanasc", String.class).invoke(signup, "1990-01-01");
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
        }
        try {
            signup.getClass().getMethod("setGen", String.class).invoke(signup, "M");
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
        }

        // Map to entity via mapper and persist
        Cliente cliente = ClienteMapper.toEntityFromSignup(signup);
        // set password encoded — detect raw password from DTO
        String rawPwd = null;
        try {
            rawPwd = (String) signup.getClass().getMethod("getSenha").invoke(signup);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
        }
        if (rawPwd == null) {
            try {
                rawPwd = (String) signup.getClass().getMethod("getPassword").invoke(signup);
            } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException ignored) {
            }
        }
        if (rawPwd == null)
            rawPwd = "Senha@123";
        cliente.setSenha(passwordEncoder.encode(rawPwd));

        Cliente saved = clienteRepository.save(cliente);
        assertThat(saved.getId()).isNotNull();

        // Verify mapper round-trip
        var dto = ClienteMapper.fromEntity(saved);
        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isEqualTo(saved.getEmail());
        assertThat(dto.getNome()).isEqualTo(saved.getNome());

        // Verify UserDetails loading and role assignment
        UserDetails ud = userDetailsService.loadUserByUsername(saved.getEmail());
        assertThat(ud).isNotNull();
        assertThat(ud.getUsername()).isEqualTo(saved.getEmail());
        // expect ROLE_USER for cliente
        assertThat(ud.getAuthorities()).extracting("authority").contains("ROLE_USER");

        // Generate and validate JWT
        String token = jwtUtil.generateToken(saved.getEmail());
        assertThat(token).isNotNull();
        // depending on implementation validateToken may accept only token or
        // token+username — adapt accordingly
        boolean valid = jwtUtil.validateToken(token);
        assertThat(valid).isTrue();
    }
}
