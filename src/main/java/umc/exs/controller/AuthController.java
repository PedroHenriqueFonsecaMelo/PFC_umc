package umc.exs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import umc.exs.service.AuthService;

// Controlador para autenticação de usuários
@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("/")
    public String showLoginPage() {
        return "index.html"; 
    }

    @PostMapping("/cadastro")
    @ResponseBody
    public String cadastro(@RequestParam String username, @RequestParam String password) {
        boolean success = authService.cadastro(username, password);
        return success ? "Usuário registrado com sucesso!" : "Usuário já existe!";
    }

    @PostMapping("/login")
    @ResponseBody
    public String login(@RequestParam String username, @RequestParam String password) {
        String status = authService.login(username, password);
        return status;
    }
}