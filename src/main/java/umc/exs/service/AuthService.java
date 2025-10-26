package umc.exs.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.exs.log.SecurityLogger;
import umc.exs.model.entidades.Cliente;
import umc.exs.repository.ClienteRepository;

@Service
public class AuthService {


    @Autowired
    private ClienteRepository userRepository;
    @Autowired
    private SecurityLogger securityLogger;

    Cliente user = new Cliente();

    public AuthService() {
    }

    public boolean cadastro(String nome, String senha) {
        if (userRepository.findByNome(nome).isPresent()) {
            return false;
        }
        String salt = BCrypt.gensalt();
        String hash = BCrypt.hashpw(senha, salt);

        
        user.setNome(nome);
        user.setSenha(hash);
        userRepository.save(user);

        return true;
    }

    public String login(String nome, String senha) {
        var userOpt = userRepository.findByNome(nome);

        if (userOpt.isEmpty()) {
            securityLogger.Flogin(nome, "Usuário não encontrado");
            return "Não há usuário com esse nome.";
        }

        user = userOpt.get();

        if (user.ContaBloqueada()) {
            securityLogger.Flogin(nome, "Conta bloqueada");
            return "Usuário bloqueado.";
        }

        if (BCrypt.checkpw(senha, user.getSenha()) && !user.ContaBloqueada()) {
            user.logado();
           
            userRepository.save(user);
            securityLogger.Slogin(nome);

            return "usuário logado com sucesso.";
        } else {

            user.setFalhas();

            if (user.ContaBloqueada()) {
                securityLogger.CBloqueada(nome);

                userRepository.save(user);

                return "Muitas tentativas falhas. Conta bloqueada.";
            }

            userRepository.save(user);
            return "senha ou usuário inválido.";
        }
    }
}
