package umc.cfc.service;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import umc.cfc.model.User;
import umc.cfc.repository.UserRepository;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;
    private int attempts = 0;

    public AuthService() {
    }

    public boolean register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("Tentativa de registro de usuário duplicado: {}", username);
            return false;
        }
        String salt = BCrypt.gensalt();
        String hash = BCrypt.hashpw(password, salt);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        userRepository.save(user);
        logger.info("Novo usuário registrado com sucesso: {}", username);

        return true;
    }

    public String login(String username, String password) {
        var userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            logger.warn("Tentativa de login falha para usuário inexistente: {}", username);
            return "Não há usuário com esse nome.";
        }

        User user = userOpt.get();

        if (user.isAccountLocked()) {
            logger.warn("Tentativa de login para usuário bloqueado: {}", username);
            return "Usuário bloqueado.";
        }

        if (BCrypt.checkpw(password, user.getPasswordHash()) && !user.isAccountLocked()) {
            user.setFailedAttempts(0);
            user.setAccountLocked(false);
            attempts = 0;

            userRepository.save(user);
            logger.info("Usuário logado com sucesso: {}", username);
            return "usuário logado com sucesso.";
        } else {
            attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            logger.warn("Tentativa de login falha para usuário: {}. Tentativas falhas: {}", username, attempts);

            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                userRepository.save(user);

                logger.error("Usuário {} bloqueado após {} tentativas falhas.", username, attempts);

                return "Muitas tentativas falhas. Conta bloqueada."
                        + (user.isAccountLocked() ? " Usuário bloqueado." : "");
            }
            userRepository.save(user);
            return "senha ou usuário inválido." + (user.isAccountLocked() ? " Usuário bloqueado." : "");
        }
    }
}
