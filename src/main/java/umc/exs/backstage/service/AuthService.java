package umc.exs.backstage.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import umc.exs.model.DTO.auth.LoginDTO;
import umc.exs.model.DTO.auth.SignupDTO;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;

@Service
public class AuthService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Cliente cadastrar(SignupDTO signupDTO) {
        if (clienteRepository.findByEmail(signupDTO.getEmail()).isPresent()) {
            throw new RuntimeException("Email já cadastrado.");
        }

        Cliente novo = new Cliente();
        novo.setNome(signupDTO.getNome());
        novo.setEmail(signupDTO.getEmail());
        novo.setSenha(passwordEncoder.encode(signupDTO.getSenha()));
        novo.setDatanasc(signupDTO.getDatanasc());
        novo.setGen(signupDTO.getGen());

        return clienteRepository.save(novo);
    }

    public Cliente autenticar(LoginDTO loginDTO) {
        Cliente cliente = clienteRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new RuntimeException("Email não encontrado."));

        if (!passwordEncoder.matches(loginDTO.getPassword(), cliente.getSenha())) {
            throw new RuntimeException("Senha incorreta.");
        }

        return cliente;
    }
}
