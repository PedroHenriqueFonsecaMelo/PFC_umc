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

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.security.JwtUtil;
import umc.exs.service.AuthHelper;
import umc.exs.service.ClienteService;

/**
 * Controller responsável pela gestão e autenticação de clientes.
 * Segue o padrão de rotas kebab-case e injeção por construtor.
 */
@Controller
@RequestMapping("/clientes")
public class ClientController {

    private final ClienteService clienteService;
    private final LogAuditoriaService logAuditoriaService;
    private final AuthHelper authHelper;
    private final JwtUtil jwtUtil;

    // Injeção de dependências via construtor: garante que o controller seja
    // imutável e fácil de testar.
    public ClientController(ClienteService clienteService,
            LogAuditoriaService logAuditoriaService,
            AuthHelper authHelper,
            JwtUtil jwtUtil) {
        this.clienteService = clienteService;
        this.logAuditoriaService = logAuditoriaService;
        this.authHelper = authHelper;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Exibe a página de cadastro.
     * Limpa o cookie JWT para garantir que não haja sessões residuais.
     */
    @GetMapping("/novo-cadastro")
    public String exibirFormularioCadastro(HttpServletResponse response, Model model) {
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new SignupDTO());
        }
        jwtUtil.clearJwtCookie(response);
        return "cliente/cadastro_cliente";
    }

    /**
     * Processa os dados do novo cliente.
     * 
     * @Valid aciona as validações do Bean Validation (NotBlank, Email, etc) no DTO.
     */
    @PostMapping("/novo-cadastro")
    public String registrarCliente(
            @Valid @ModelAttribute("cliente") SignupDTO signupDTO,
            BindingResult result,
            @RequestParam(defaultValue = "false") boolean termsAccepted,
            @RequestParam(defaultValue = "false") boolean privacyAccepted,
            @RequestParam String confirmPassword,
            Model model,
            HttpServletResponse response) {

        // Validações de regra de negócio que não cabem no Bean Validation
        if (!termsAccepted || !privacyAccepted) {
            model.addAttribute("erro", "Você precisa aceitar os termos e políticas de privacidade.");
            return "cliente/cadastro_cliente";
        }

        if (!signupDTO.getSenha().equals(confirmPassword)) {
            model.addAttribute("erro", "As senhas não coincidem.");
            return "cliente/cadastro_cliente";
        }

        // Verifica se houve erros de validação nos campos do DTO
        if (result.hasErrors()) {
            return "cliente/cadastro_cliente";
        }

        try {
            ClienteDTO salvo = clienteService.salvarCliente(signupDTO);
            // Autentica automaticamente e gera o cookie após o cadastro
            authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");
            return "redirect:/clientes/meu-perfil";
        } catch (Exception e) {
            logAuditoriaService.registrarLog("CADASTRO_FALHA", 0L, signupDTO.getEmail(), e.getMessage());
            model.addAttribute("erro", "Falha ao cadastrar: " + e.getMessage());
            return "cliente/cadastro_cliente";
        }
    }

    /**
     * Exibe a tela de login.
     */
    @GetMapping("/login")
    public String exibirLogin(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginDTO());
        }
        return "cliente/login_cliente";
    }

    /**
     * Processa a tentativa de login.
     */
    @PostMapping("/login")
    public String realizarLogin(@ModelAttribute("loginData") LoginDTO loginDTO, Model model,
            HttpServletResponse response) {
        if (loginDTO == null || loginDTO.getEmail() == null) {
            model.addAttribute("erro", "Preencha os campos corretamente.");
            return "cliente/login_cliente";
        }

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(loginDTO.getEmail(), loginDTO.getSenha());

        if (clienteOpt.isEmpty()) {
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO.getEmail(), "Senha incorreta.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();
        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");

        return "redirect:/clientes/meu-perfil";
    }

    /**
     * Finaliza a sessão do usuário.
     * 
     * @AuthenticationPrincipal injeta os dados do usuário logado diretamente do
     *                          contexto do Spring Security.
     */
    @GetMapping("/sair")
    public String deslogar(HttpServletResponse response, @AuthenticationPrincipal UserDetails user) {
        if (user != null) {
            String email = user.getUsername();
            Long clienteId = clienteService.buscarClientePorEmail(email).map(ClienteDTO::getId).orElse(0L);
            logAuditoriaService.registrarLog("LOGOUT_SUCESSO", clienteId, email, "Sessão encerrada pelo usuário.");
        }

        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    /**
     * Exibe o dashboard/perfil do cliente logado.
     */
    @GetMapping("/meu-perfil")
    public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null) {
            return "redirect:/clientes/login";
        }

        String email = user.getUsername();
        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(email)
                .orElseThrow(() -> new RuntimeException("Dados do perfil não encontrados."));

        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

    @GetMapping("/tokens")
    public String abrirPaginaCompra() {
        return "cliente/carteira"; // Nome do seu arquivo HTML em templates/cliente/
    }
}