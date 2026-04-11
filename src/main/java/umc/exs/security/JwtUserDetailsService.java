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
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Service
@RequiredArgsConstructor
public class JwtUserDetailsService implements UserDetailsService {

    private final ClienteRepository clienteRepository;
    private final AdminRepository adminRepository;

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

        return User.withUsername(c.getEmail())
                .password(c.getSenha())
                .authorities("ROLE_USER")
                .build();
    }
}
