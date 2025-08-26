package umc.cfc.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import umc.cfc.model.User;
import umc.cfc.repository.UserRepository;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepository;

    public boolean register(String username, String password) {
        if (userRepository.findByUsername(username).isPresent()) {
            return false; // Usuário já existe
        }
        String salt = BCrypt.gensalt();
        String hash = BCrypt.hashpw(password, salt);
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        userRepository.save(user);
        return true;
    }

    public boolean login(String username, String password) {
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) return false;
        User user = userOpt.get();
        if (user.isAccountLocked()) return false;
        if (BCrypt.checkpw(password, user.getPasswordHash())) {
            user.setFailedAttempts(0);
            userRepository.save(user);
            return true;
        } else {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
            }
            userRepository.save(user);
            return false;
        }
    }
}
