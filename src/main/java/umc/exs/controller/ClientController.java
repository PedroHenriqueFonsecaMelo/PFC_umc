package umc.exs.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.log.LogAuditoriaService;
import umc.exs.security.JwtUtil;
import umc.exs.service.AuthHelper;
import umc.exs.service.ClienteService;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.ClienteDTO;

@Controller
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private LogAuditoriaService logAuditoriaService;

    @Autowired
    private AuthHelper authHelper;

    @Autowired
    private JwtUtil jwtCookieHelper;

    @GetMapping("/cadastro")
    public String mostrarCadastro(HttpServletResponse response, Model model) {
        if (!model.containsAttribute("cliente"))
            model.addAttribute("cliente", new SignupDTO());
        jwtCookieHelper.clearJwtCookie(response);
        return "cliente/cadastro_cliente";
    }

    @PostMapping("/cadastro")
    public String cadastrarCliente(@ModelAttribute SignupDTO signupDTO, @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            Model model, HttpServletResponse response) {

        if (Boolean.FALSE.equals(termsAccepted) || Boolean.FALSE.equals(privacyAccepted)) {
            model.addAttribute("erro", "É necessário aceitar os termos e a política de privacidade.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }
        if ((signupDTO.getSenha() == null ? confirmPassword != null : !signupDTO.getSenha().equals(confirmPassword))) {
            model.addAttribute("erro", "As senhas devem ser iguais");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);

        if (salvo == null || salvo.getId() == null) {
            logAuditoriaService.registrarLog("CADASTRO_FALHA", 0L, signupDTO.getEmail(), "Erro interno ao persistir cliente.");
            throw new RuntimeException("Erro interno ao concluir o cadastro. Tente novamente.");
        }

        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("loginData"))
            model.addAttribute("loginData", new LoginDTO());
        return "cliente/login_cliente";
    }

    @PostMapping("/login")
    public String loginCliente(@ModelAttribute("loginData") LoginDTO loginDTO, Model model, HttpServletResponse response) {
        if (loginDTO == null || loginDTO.getEmail() == null || loginDTO.getSenha() == null) {
            model.addAttribute("erro", "Dados inválidos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO != null ? loginDTO.getEmail() : "NULL_EMAIL",
                    "Tentativa de login com dados nulos/vazios.");
            return "cliente/login_cliente";
        }

        String email = loginDTO.getEmail();

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(email, loginDTO.getSenha());

        if (clienteOpt.isEmpty()) {
            model.addAttribute("erro", "Email ou senha incorretos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, email, "Credenciais inválidas.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();

        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");
        return "redirect:/clientes/homepage";
    }

    @GetMapping("/logout")
    public String logoutCliente(HttpServletResponse response, Principal principal) {
        String email = principal != null ? principal.getName() : "DESCONHECIDO";
        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorEmail(email);
        Long clienteId = clienteOpt.map(ClienteDTO::getId).orElse(0L);

        logAuditoriaService.registrarLog("LOGOUT_SUCESSO", clienteId, email, "Usuário deslogou do sistema.");

        jwtCookieHelper.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    @GetMapping("/homepage")
    public String getHomepage(Principal principal, Model model) {
        String emailDoClienteLogado = principal.getName();

        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado. Por favor, faça login novamente."));

        if (clienteDTO.getEnderecos() == null) {
            clienteDTO.setEnderecos(new ArrayList<>());
        }

        if (clienteDTO.getCartoes() == null) {
            clienteDTO.setCartoes(new ArrayList<>());
        }
        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

}
