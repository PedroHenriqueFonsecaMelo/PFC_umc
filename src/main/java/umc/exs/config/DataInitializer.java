package umc.exs.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import umc.exs.repository.AdminRepository;
import umc.exs.repository.ClienteRepository;
import umc.exs.repository.PontuacaoUsuarioRepository;

import umc.exs.model.entidades.foundation.Administrador;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.foundation.PontuacaoUsuario;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepo;
    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final PasswordEncoder encoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (adminRepo.count() == 0) {
            createAdmin();
            createClients();
            createPontuacao();
            log.info("✅ DataInitializer: Core seed data loaded (admin, clients, pontuacao).");
        }
    }

    private void createAdmin() {
        Administrador admin = new Administrador();
        admin.setNome("Admin Master");
        admin.setEmail("admin@admin.com");
        admin.setPassword(encoder.encode("admin123"));
        adminRepo.save(admin);
    }

    private void createClients() {
        Cliente c1 = new Cliente();
        c1.setNome("Cliente Teste");
        c1.setEmail("cliente@teste.com");
        c1.setSenha(encoder.encode("admin123"));
        c1.setSaldoTokens(5000.0);
        c1.setBloqueada(false);
        c1.setCpf("12345678900");
        clienteRepo.save(c1);

        Cliente c2 = new Cliente();
        c2.setNome("Maria Leitora");
        c2.setEmail("maria@teste.com");
        c2.setSenha(encoder.encode("admin123"));
        c2.setSaldoTokens(200.0);
        c2.setBloqueada(false);
        c2.setCpf("98765432100");
        clienteRepo.save(c2);

        Cliente c3 = new Cliente();
        c3.setNome("Pedro Leitor");
        c3.setEmail("pedro@teste.com");
        c3.setSenha(encoder.encode("admin123"));
        c3.setSaldoTokens(80.0);
        c3.setBloqueada(false);
        c3.setCpf("11122233344");
        clienteRepo.save(c3);
    }

    private void createPontuacao() {
        // p1 for cliente1
        if (pontuacaoRepo.findByClienteEmail("cliente@teste.com").isEmpty()) {
            PontuacaoUsuario p1 = new PontuacaoUsuario();
            p1.setXpTotal(150);
            pontuacaoRepo.save(p1);
        }

        // p2 for maria
        if (pontuacaoRepo.findByClienteEmail("maria@teste.com").isEmpty()) {
            PontuacaoUsuario p2 = new PontuacaoUsuario();
            p2.setXpTotal(80);
            pontuacaoRepo.save(p2);
        }

        // p3 for pedro
        if (pontuacaoRepo.findByClienteEmail("pedro@teste.com").isEmpty()) {
            PontuacaoUsuario p3 = new PontuacaoUsuario();
            p3.setXpTotal(45);
            pontuacaoRepo.save(p3);
        }
    }
}
