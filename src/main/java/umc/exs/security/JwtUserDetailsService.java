package umc.exs.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.StatusConta;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.usuario.ClienteRepository;

/**
 * Implementação do UserDetailsService do Spring Security responsável por carregar os dados
 * de autenticação dos usuários. Administradores recebem a role ADMIN e clientes recebem
 * a role USER. Contas com status SUSPENSO ou REMOVIDO são bloqueadas durante o carregamento.
 */
@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final AdminRepository adminRepository;

    /**
     * Busca o usuário pelo e-mail, verificando primeiro na base de administradores e
     * depois na de clientes. Lança UsernameNotFoundException se o usuário não for
     * encontrado ou se a conta estiver com status SUSPENSO ou REMOVIDO.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // First try to find an admin
        Optional<Administrador> optAdmin = adminRepository.findByEmail(username);
        if (optAdmin.isPresent()) {
            Administrador admin = optAdmin.get();
            return User.withUsername(admin.getEmail())
                    .password(admin.getPassword())
                    .authorities("ROLE_ADMIN", "ADMIN")
                    .build();
        }

        // Then try to find a regular client
        Optional<Cliente> opt = clienteRepository.findByEmail(username);
        Cliente c = opt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        StatusConta status = c.getStatusConta() != null ? c.getStatusConta() : StatusConta.ATIVO;
        if (status == StatusConta.SUSPENSO || status == StatusConta.REMOVIDO) {
            throw new UsernameNotFoundException("Conta " + status.name().toLowerCase() + ": " + username);
        }

        return User.withUsername(c.getEmail())
                .password(c.getSenha())
                .authorities("ROLE_USER")
                .build();
    }
}
