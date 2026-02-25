package umc.exs.controller;

import java.util.Optional;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.SenhaResetDTO;
import umc.exs.security.JwtUtil;
import umc.exs.service.AuthHelper;
import umc.exs.service.ClienteService;

@Controller
@RequestMapping("/clientes")
public class ClientController {

    private final ClienteService clienteService;
    private final LogAuditoriaService logAuditoriaService;
    private final AuthHelper authHelper;
    private final JwtUtil jwtUtil;

    public ClientController(ClienteService clienteService,
                            LogAuditoriaService logAuditoriaService,
                            AuthHelper authHelper,
                            JwtUtil jwtUtil) {
        this.clienteService = clienteService;
        this.logAuditoriaService = logAuditoriaService;
        this.authHelper = authHelper;
        this.jwtUtil = jwtUtil;
    }

    // --- CADASTRO E LOGIN ---

    @GetMapping("/novo-cadastro")
    public String exibirFormularioCadastro(HttpServletResponse response, Model model) {
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new SignupDTO());
        }
        jwtUtil.clearJwtCookie(response);
        return "cliente/cadastro_cliente";
    }

    @PostMapping("/novo-cadastro")
    public String registrarCliente(
            @Valid @ModelAttribute("cliente") SignupDTO signupDTO,
            BindingResult result,
            @RequestParam String confirmPassword,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(signupDTO.getTermsAccepted())
                || Boolean.FALSE.equals(signupDTO.getPrivacyAccepted())) {
            model.addAttribute("erro", "Você precisa aceitar os termos e políticas de privacidade.");
            return "cliente/cadastro_cliente";
        }

        if (!signupDTO.getSenha().equals(confirmPassword)) {
            result.rejectValue("senha", "error.senha", "As senhas não coincidem.");
        }

        if (result.hasErrors()) {
            return "cliente/cadastro_cliente";
        }

        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);
        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");

        return "redirect:/clientes/meu-perfil";
    }

    @GetMapping("/login")
    public String exibirLogin(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginDTO());
        }
        return "cliente/login_cliente";
    }

    @PostMapping("/login")
    public String realizarLogin(
            @Valid @ModelAttribute("loginData") LoginDTO loginDTO,
            BindingResult result,
            Model model,
            HttpServletResponse response) {

        if (result.hasErrors()) {
            return "cliente/login_cliente";
        }

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(loginDTO.getEmail(), loginDTO.getSenha());

        if (clienteOpt.isEmpty()) {
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO.getEmail(), "Credenciais inválidas.");
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();
        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");

        return "redirect:/clientes/meu-perfil";
    }

    @GetMapping("/sair")
    public String deslogar(HttpServletResponse response, @AuthenticationPrincipal UserDetails user) {
        if (user != null) {
            String email = user.getUsername();
            Long clienteId = clienteService.buscarClientePorEmail(email).map(ClienteDTO::getId).orElse(0L);
            logAuditoriaService.registrarLog("LOGOUT_SUCESSO", clienteId, email, "Sessão encerrada.");
        }

        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // --- PERFIL E CARTEIRA ---

    @GetMapping("/meu-perfil")
    public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return "redirect:/clientes/login";

        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));

        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

    // --- RECUPERAÇÃO DE SENHA ---

    @GetMapping("/recuperar-senha")
    public String mostrarPaginaRecuperarSenha() {
        return "cliente/recuperar_senha";
    }

    @PostMapping("/recuperar-senha")
    public String iniciarRecuperacaoSenha(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            if (email == null || email.isBlank()) {
                redirectAttributes.addFlashAttribute("erro", "O email não pode ser vazio.");
                return "redirect:/clientes/login";
            }

            clienteService.iniciarRecuperacaoSenha(email);

            logAuditoriaService.registrarLog("SENHA_RECU_INICIO", 0L, email, "Processo iniciado.");
            redirectAttributes.addFlashAttribute("sucesso", "Um link para resetar sua senha foi enviado para o seu email.");

        } catch (IllegalArgumentException e) {
            // Mensagem genérica por segurança (evita enumeração de usuários)
            redirectAttributes.addFlashAttribute("sucesso", "Se o email existir em nosso sistema, um link foi enviado.");
            logAuditoriaService.registrarLog("SENHA_RECU_FALHA", 0L, email, "Email não encontrado.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao processar a solicitação");
            logAuditoriaService.registrarLog("SENHA_RECU_FALHA", 0L, email, "Erro inesperado.");
        }

        return "redirect:/clientes/login";
    }

    @GetMapping("/reset-senha")
    public String mostrarFormularioResetSenha(@RequestParam("token") String token, Model model) {
        try {
            boolean tokenValido = clienteService.validarTokenRecuperacao(token);

            if (!tokenValido) {
                model.addAttribute("erro", "Token inválido ou expirado.");
                logAuditoriaService.registrarLog("SENHA_RESET_TOKEN_FALHA", 0L, "TOKEN_INV", "Token: " + token);
                return "cliente/login_cliente";
            }

            model.addAttribute("resetData", new SenhaResetDTO(token, null, null));
            model.addAttribute("tokenValido", true);
            return "cliente/reset_senha";

        } catch (Exception e) {
            model.addAttribute("erro", "Ocorreu um erro ao validar o token");
            return "cliente/login_cliente";
        }
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@ModelAttribute("resetData") SenhaResetDTO resetDTO,
            RedirectAttributes redirectAttributes) {

        String token = resetDTO.getToken();
        String novaSenha = resetDTO.getNovaSenha();
        String confirmarSenha = resetDTO.getConfirmarSenha();

        try {
            if (token == null || token.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Token ausente.");
                return "redirect:/clientes/login";
            }

            if (novaSenha == null || novaSenha.isEmpty() || !novaSenha.equals(confirmarSenha)) {
                redirectAttributes.addFlashAttribute("erro", "As senhas não conferem.");
                return "redirect:/clientes/reset-senha?token=" + token;
            }

            String emailDoCliente = clienteService.alterarSenhaComToken(token, novaSenha);

            logAuditoriaService.registrarLog("SENHA_ALTERADA_SUCESSO", 0L, emailDoCliente, "Sucesso via token.");
            redirectAttributes.addFlashAttribute("sucesso", "Senha alterada com sucesso! Faça login.");

            return "redirect:/clientes/login";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao alterar a senha: " + e.getMessage());
            return "redirect:/clientes/login";
        }
    }
}