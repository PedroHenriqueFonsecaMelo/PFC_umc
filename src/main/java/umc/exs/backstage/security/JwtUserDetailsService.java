package umc.exs.backstage.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import umc.exs.model.daos.repository.AdminRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.entidades.foundation.Administrador;
import umc.exs.model.entidades.usuario.Cliente;

import java.util.List;
import java.util.Optional;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try admin first
        Optional<Administrador> adminOpt = adminRepository.findByEmail(username);
        if (adminOpt.isPresent()) {
            Administrador adm = adminOpt.get();
            String pwd = adm.getPassword() != null ? adm.getPassword() : "";
            return new User(adm.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        Optional<Cliente> cliOpt = clienteRepository.findByEmail(username);
        if (cliOpt.isEmpty()) throw new UsernameNotFoundException("User not found: " + username);
        Cliente c = cliOpt.get();
        String pwd = c.getSenha() != null ? c.getSenha() : "";
        return new User(c.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserDetails loadUserByUserId(Long userId) throws UsernameNotFoundException {
        // Try admin first
        Optional<Administrador> adminOpt = adminRepository.findById(userId);
        if (adminOpt.isPresent()) {
            Administrador adm = adminOpt.get();
            String pwd = adm.getPassword() != null ? adm.getPassword() : "";
            return new User(adm.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        Optional<Cliente> cliOpt = clienteRepository.findById(userId);
        if (cliOpt.isEmpty()) throw new UsernameNotFoundException("User not found with ID: " + userId);
        Cliente c = cliOpt.get();
        String pwd = c.getSenha() != null ? c.getSenha() : "";
        return new User(c.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    public UserDetails LoadUserByUserEmailAndId (String email, Long id) throws UsernameNotFoundException {
        // Try admin first
        Optional<Administrador> adminOpt = adminRepository.findByEmailAndId(email, id);
        if (adminOpt.isPresent()) {
            Administrador adm = adminOpt.get();
            String pwd = adm.getPassword() != null ? adm.getPassword() : "";
            return new User(adm.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        }

        Optional<Cliente> cliOpt = clienteRepository.findByEmailAndId(email, id);
        if (cliOpt.isEmpty()) throw new UsernameNotFoundException("User not found with email and ID: " + email + ", " + id);
        Cliente c = cliOpt.get();
        String pwd = c.getSenha() != null ? c.getSenha() : "";
        return new User(c.getEmail(), pwd, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}