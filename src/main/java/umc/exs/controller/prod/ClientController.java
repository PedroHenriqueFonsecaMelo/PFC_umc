package umc.exs.controller.prod;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.backstage.log.LogAuditoriaService;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.AuthHelper;
import umc.exs.backstage.service.ClienteService;
import umc.exs.model.dtos.auth.LoginDTO;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;

import java.security.Principal;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    // ============================================================
    // 🔹 CADASTRO (Removido try-catch - Exceções tratadas globalmente)
    // ============================================================

    @GetMapping("/cadastro")
    public String mostrarCadastro(Model model) {
        if (!model.containsAttribute("cliente"))
            model.addAttribute("cliente", new SignupDTO()); 
        return "cliente/cadastro_cliente";
    }

    @PostMapping("/cadastro")
    public String cadastrarCliente(
            @ModelAttribute SignupDTO signupDTO,
            @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(termsAccepted) || Boolean.FALSE.equals(privacyAccepted)) {
            // Este é um erro específico de UI, tratado localmente.
            model.addAttribute("erro", "É necessário aceitar os termos e a política de privacidade.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        String rawEmail = signupDTO.getEmail();
        
        // Chamada ao Service. Se houver erro de validação (Email, Senha, CPF, etc.),
        // o Service lança uma IllegalArgumentException, capturada pelo GlobalExceptionHandler.
        ClienteDTO salvo = clienteService.salvarCliente(signupDTO); 
        
        // Se a validação passou, mas houve erro interno de persistência
        if (salvo == null || salvo.getId() == null) {
            logAuditoriaService.registrarLog("CADASTRO_FALHA", 0L, rawEmail, "Erro interno ao persistir cliente.");
            // Lança uma exceção para que o GlobalExceptionHandler trate o redirecionamento
            throw new RuntimeException("Erro interno ao concluir o cadastro. Tente novamente.");
        }

        // Lógica de Autenticação, Cookie e Log centralizada no AuthHelper
        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    @PostMapping("/cadastro-completo")
    public String cadastrarClienteCompleto(
            @ModelAttribute SignupDTO signupDTO,
            @ModelAttribute EnderecoDTO enderecoDTO,
            @ModelAttribute CartaoDTO cartaoDTO,
            @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(termsAccepted) || Boolean.FALSE.equals(privacyAccepted)) {
            model.addAttribute("erro", "É necessário aceitar os termos e a política de privacidade.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        String rawEmail = signupDTO.getEmail();

        // O Service trata todas as validações de signup, endereco e cartao
        ClienteDTO salvo = clienteService.salvarClienteCompleto(signupDTO, enderecoDTO, cartaoDTO);
        
        if (salvo == null || salvo.getId() == null) {
            logAuditoriaService.registrarLog("CADASTRO_COMPLETO_FALHA", 0L, rawEmail, "Erro interno ao persistir cliente e associações.");
            throw new RuntimeException("Erro interno ao concluir o cadastro completo. Tente novamente.");
        }

        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_COMPLETO_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    // ==========================================================================
    // 🔹 LOGIN (Mantido tratamento local para Credenciais Inválidas)
    // ==========================================================================

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("loginData"))
            model.addAttribute("loginData", new LoginDTO()); 
        return "cliente/login_cliente";
    }

    @PostMapping("/login")
    public String loginCliente(@ModelAttribute("loginData") LoginDTO loginDTO,
            Model model, HttpServletResponse response) {

        if (loginDTO == null || loginDTO.getEmail() == null || loginDTO.getSenha() == null) {
            model.addAttribute("erro", "Dados inválidos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO != null ? loginDTO.getEmail() : "NULL_EMAIL", "Tentativa de login com dados nulos/vazios.");
            return "cliente/login_cliente";
        }
        
        String email = loginDTO.getEmail();
        
        // Autentica o cliente. Se as credenciais estiverem erradas, retorna Optional.empty.
        // Se houver um erro de validação de email no service, ele lançará IllegalArgumentException
        // que será pego pelo GlobalExceptionHandler.
        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(email, loginDTO.getSenha());

        if (clienteOpt.isEmpty()) {
            // Tratamento de falha de credencial (Email ou senha incorretos)
            model.addAttribute("erro", "Email ou senha incorretos."); 
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();
        
        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    @PostMapping("/logout")
    public String logoutCliente(HttpServletResponse response, Principal principal) {
        String email = principal != null ? principal.getName() : "DESCONHECIDO";
        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorEmail(email);
        Long clienteId = clienteOpt.map(ClienteDTO::getId).orElse(0L);

        logAuditoriaService.registrarLog("LOGOUT_SUCESSO", clienteId, email, "Usuário deslogou do sistema.");
        
        jwtCookieHelper.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // ==========================================================
    // 🏠 HOMEPAGE / CARREGAMENTO DO CLIENTE (GET)
    // ==========================================================
    @GetMapping("/homepage")
    public String getHomepage(Principal principal, Model model) {
        String emailDoClienteLogado = principal.getName();
        // Assume que este método lança exceção se o cliente não for encontrado
        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado. Por favor, faça login novamente."));

        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

    // ==========================================================
    // 💾 ATUALIZAR CLIENTE (Removido try-catch - Exceções tratadas globalmente)
    // ==========================================================
    @PostMapping("/atualizar")
    public String atualizarCliente(
            @ModelAttribute("cliente") ClienteDTO clienteAtualizadoDTO,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .map(ClienteDTO::getId)
                .orElse(0L); // ClienteId pode ser 0L se a busca falhar

        try {
            // Chamada do Service para atualização. Lança IllegalArgumentException ou RuntimeException.
            clienteService.atualizarClienteEAssociacoes(clienteId, clienteAtualizadoDTO); 

            logAuditoriaService.registrarLog("CLIENTE_ATUALIZACAO", clienteId, emailDoClienteLogado, "Dados básicos e associações atualizadas com sucesso.");
            redirectAttributes.addFlashAttribute("sucesso", "Suas informações foram atualizadas com sucesso!");

        } catch (Exception e) {
            // Mantendo o log de falha no Controller. A exceção será relançada para o Handler
            logAuditoriaService.registrarLog("CLIENTE_ATUALIZACAO_FALHA", clienteId, emailDoClienteLogado, "Erro ao atualizar informações: " + e.getMessage());
            // Relança a exceção para que o GlobalExceptionHandler a capture e use o Referer
            throw e; 
        }

        return "redirect:/clientes/homepage";
    }


    // ==========================================================
    // 🗑️ DELEÇÃO DE ENDEREÇO/CARTÃO/CONTA (Removido try-catch)
    // ==========================================================
    @GetMapping("/removerEndereco")
    public String removerEndereco(@RequestParam("enderecoId") Long enderecoId, Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
            .map(ClienteDTO::getId).orElse(0L);

        try {
            clienteService.deletarEnderecoDoCliente(clienteId, enderecoId);
            logAuditoriaService.registrarLog("ENDERECO_DELECAO_SUCESSO", clienteId, emailDoClienteLogado, "Endereço ID " + enderecoId + " removido.");
            redirectAttributes.addFlashAttribute("sucesso", "Endereço removido com sucesso!");
        } catch (Exception e) {
            logAuditoriaService.registrarLog("ENDERECO_DELECAO_FALHA", clienteId, emailDoClienteLogado, "Erro ao remover endereço ID " + enderecoId + ": " + e.getMessage());
            throw e;
        }

        return "redirect:/clientes/homepage";
    }

    @PostMapping("/removerCartao")
    public String removerCartao(@RequestParam("cartaoId") Long cartaoId, Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
            .map(ClienteDTO::getId).orElse(0L);

        try {
            clienteService.deletarCartaoDoCliente(clienteId, cartaoId);
            logAuditoriaService.registrarLog("CARTAO_DELECAO_SUCESSO", clienteId, emailDoClienteLogado, "Cartão ID " + cartaoId + " removido.");
            redirectAttributes.addFlashAttribute("sucesso", "Cartão removido com sucesso!");
        } catch (Exception e) {
            logAuditoriaService.registrarLog("CARTAO_DELECAO_FALHA", clienteId, emailDoClienteLogado, "Erro ao remover cartão ID " + cartaoId + ": " + e.getMessage());
            throw e;
        }

        return "redirect:/clientes/homepage";
    }

    @PostMapping("/deletar")
    public String deletarCliente(Principal principal, HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
            .map(ClienteDTO::getId).orElse(0L);
        
        try {
            clienteService.deletarClientePorId(clienteId);

            logAuditoriaService.registrarLog("CONTA_DELECAO_SUCESSO", clienteId, emailDoClienteLogado, "Conta do cliente deletada com sucesso.");

            jwtCookieHelper.clearJwtCookie(response);
            SecurityContextHolder.clearContext();

            redirectAttributes.addFlashAttribute("sucesso", "Conta deletada com sucesso!");
            return "redirect:/";
        } catch (Exception e) {
            logAuditoriaService.registrarLog("CONTA_DELECAO_FALHA", clienteId, emailDoClienteLogado, "Erro ao deletar conta: " + e.getMessage());
            throw e; // Lançar para o GlobalExceptionHandler
        }
    }
}