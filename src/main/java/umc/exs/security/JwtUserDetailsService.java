package umc.exs.security;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.entidades.usuario.Cliente;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Cliente> opt = clienteRepository.findByEmail(username);
        Cliente c = opt.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return User.withUsername(c.getEmail())
                .password(c.getSenha())
                .authorities("ROLE_USER")
                .build();
    }
}
