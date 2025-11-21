package umc.exs.controller.prod;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
import umc.exs.model.dtos.user.SenhaResetDTO;

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

    /** 
     * @param response
     * @param model
     * @return String
     */
    // ============================================================
    // 🔹 CADASTRO
    // ============================================================

    @GetMapping("/cadastro")
    public String mostrarCadastro(HttpServletResponse response, Model model) {
        if (!model.containsAttribute("cliente"))
            model.addAttribute("cliente", new SignupDTO());
        jwtCookieHelper.clearJwtCookie(response);
        return "cliente/cadastro_cliente";
    }

    /** 
     * @param model
     * @param response
     * @return String
     */
    @PostMapping("/cadastro")
    public String cadastrarCliente(
            @ModelAttribute SignupDTO signupDTO,
            @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            Model model,
            HttpServletResponse response) {

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

        // Chamada ao Service. Se houver erro de validação (IllegalArgumentException),
        // será capturada pelo GlobalExceptionHandler.
        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);

        // Verificação de segurança adicional após Service (caso o Service retorne nulo)
        if (salvo == null || salvo.getId() == null) {
            logAuditoriaService.registrarLog("CADASTRO_FALHA", 0L, signupDTO.getEmail(),
                    "Erro interno ao persistir cliente.");
            throw new RuntimeException("Erro interno ao concluir o cadastro. Tente novamente.");
        }

        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    /** 
     * @param model
     * @param response
     * @return String
     */
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

        // O Service trata todas as validações de signup, endereco e cartao
        ClienteDTO salvo = clienteService.salvarClienteCompleto(signupDTO, enderecoDTO, cartaoDTO);

        if (salvo == null || salvo.getId() == null) {
            logAuditoriaService.registrarLog("CADASTRO_COMPLETO_FALHA", 0L, signupDTO.getEmail(),
                    "Erro interno ao persistir cliente e associações.");
            throw new RuntimeException("Erro interno ao concluir o cadastro completo. Tente novamente.");
        }

        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_COMPLETO_SUCESSO");

        return "redirect:/clientes/homepage";
    }

    /** 
     * @param model
     * @return String
     */
    // ==========================================================================
    // 🔹 LOGIN/LOGOUT
    // ==========================================================================

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("loginData"))
            model.addAttribute("loginData", new LoginDTO());
        return "cliente/login_cliente";
    }

    /** 
     * @param loginDTO
     * @param model
     * @param response
     * @return String
     */
    @PostMapping("/login")
    public String loginCliente(@ModelAttribute("loginData") LoginDTO loginDTO,
            Model model, HttpServletResponse response) {

        if (loginDTO == null || loginDTO.getEmail() == null || loginDTO.getSenha() == null) {
            model.addAttribute("erro", "Dados inválidos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO != null ? loginDTO.getEmail() : "NULL_EMAIL",
                    "Tentativa de login com dados nulos/vazios.");
            return "cliente/login_cliente";
        }

        String email = loginDTO.getEmail();

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(email, loginDTO.getSenha());

        if (clienteOpt.isEmpty()) {
            // Tratamento de falha de credencial (Email ou senha incorretos)
            model.addAttribute("erro", "Email ou senha incorretos.");
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, email, "Credenciais inválidas.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();

        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");
        return "redirect:/clientes/homepage";
    }

    /** 
     * @param response
     * @param principal
     * @return String
     */
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

    /** 
     * @param principal
     * @param model
     * @return String
     */
    // ==========================================================
    // 🏠 HOMEPAGE / CARREGAMENTO DO CLIENTE (GET)
    // ==========================================================
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

    /** 
     * @param principal
     * @param redirectAttributes
     * @return String
     */
    // ==========================================================
    // 💾 ATUALIZAR CLIENTE
    // ==========================================================
    @PostMapping("/atualizar")
    public String atualizarCliente(
            @ModelAttribute("cliente") ClienteDTO clienteAtualizadoDTO,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        // Garante que o ID usado para atualização é o ID do usuário logado
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .map(ClienteDTO::getId)
                .orElseThrow(() -> new RuntimeException("Cliente logado não encontrado no sistema."));

        try {
            clienteService.atualizarClienteEAssociacoes(clienteId, clienteAtualizadoDTO);

            logAuditoriaService.registrarLog("CLIENTE_ATUALIZACAO", clienteId, emailDoClienteLogado,
                    "Dados básicos e associações atualizadas com sucesso.");
            redirectAttributes.addFlashAttribute("sucesso", "Suas informações foram atualizadas com sucesso!");

        } catch (Exception e) {
            logAuditoriaService.registrarLog("CLIENTE_ATUALIZACAO_FALHA", clienteId, emailDoClienteLogado,
                    "Erro ao atualizar informações");
            // Relança a exceção para que o GlobalExceptionHandler a capture.
            throw e;
        }

        return "redirect:/clientes/homepage";
    }

    /** 
     * @param enderecoId
     * @param principal
     * @param redirectAttributes
     * @return String
     */
    // ==========================================================
    // 🗑️ DELEÇÃO DE ENDEREÇO/CARTÃO/CONTA
    // ==========================================================

    // CORRIGIDO: Deve ser POST para operações destrutivas
    @PostMapping("/removerEndereco")
    public String removerEndereco(@RequestParam("enderecoId") Long enderecoId, Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .map(ClienteDTO::getId).orElseThrow(() -> new RuntimeException("Cliente logado não encontrado."));

        try {
            clienteService.deletarEnderecoDoCliente(clienteId, enderecoId);
            logAuditoriaService.registrarLog("ENDERECO_DELECAO_SUCESSO", clienteId, emailDoClienteLogado,
                    "Endereço ID " + enderecoId + " removido.");
            redirectAttributes.addFlashAttribute("sucesso", "Endereço removido com sucesso!");
        } catch (Exception e) {
            logAuditoriaService.registrarLog("ENDERECO_DELECAO_FALHA", clienteId, emailDoClienteLogado,
                    "Erro ao remover endereço ID " + enderecoId + "");
            throw e;
        }

        return "redirect:/clientes/homepage";
    }

    /** 
     * @param cartaoId
     * @param principal
     * @param redirectAttributes
     * @return String
     */
    @PostMapping("/removerCartao")
    public String removerCartao(@RequestParam("cartaoId") Long cartaoId, Principal principal,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .map(ClienteDTO::getId).orElseThrow(() -> new RuntimeException("Cliente logado não encontrado."));

        try {
            clienteService.deletarCartaoDoCliente(clienteId, cartaoId);
            logAuditoriaService.registrarLog("CARTAO_DELECAO_SUCESSO", clienteId, emailDoClienteLogado,
                    "Cartão ID " + cartaoId + " removido.");
            redirectAttributes.addFlashAttribute("sucesso", "Cartão removido com sucesso!");
        } catch (Exception e) {
            logAuditoriaService.registrarLog("CARTAO_DELECAO_FALHA", clienteId, emailDoClienteLogado,
                    "Erro ao remover cartão ID " + cartaoId + "");
            throw e;
        }

        return "redirect:/clientes/homepage";
    }

    /** 
     * @param principal
     * @param response
     * @param redirectAttributes
     * @return String
     */
    @PostMapping("/deletar")
    public String deletarCliente(Principal principal, HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        String emailDoClienteLogado = principal.getName();
        Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .map(ClienteDTO::getId).orElseThrow(() -> new RuntimeException("Cliente logado não encontrado."));

        try {
            clienteService.deletarClientePorId(clienteId);

            logAuditoriaService.registrarLog("CONTA_DELECAO_SUCESSO", clienteId, emailDoClienteLogado,
                    "Conta do cliente deletada com sucesso.");

            jwtCookieHelper.clearJwtCookie(response);
            SecurityContextHolder.clearContext();

            redirectAttributes.addFlashAttribute("sucesso", "Conta deletada com sucesso! Você foi desconectado.");
            return "redirect:/";
        } catch (Exception e) {
            logAuditoriaService.registrarLog("CONTA_DELECAO_FALHA", clienteId, emailDoClienteLogado,
                    "Erro ao deletar conta");
            throw e; // Lançar para o GlobalExceptionHandler
        }
    }

    /** 
     * @return String
     */
    // ==========================================================================
    // 🔑 RECUPERAÇÃO DE SENHA
    // ==========================================================================

    @GetMapping("/recuperar-senha")
    public String mostrarPaginaRecuperarSenha() {
        return "cliente/recuperar_senha";
    }

    /** 
     * @param email
     * @param redirectAttributes
     * @return String
     */
    @PostMapping("/recuperar-senha")
    public String iniciarRecuperacaoSenha(@RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
        try {
            if (email == null || email.isBlank()) {
                redirectAttributes.addFlashAttribute("erro", "O email não pode ser vazio.");
                return "redirect:/clientes/login";
            }

            // Inicia o processo: gera token, salva e envia o email
            clienteService.iniciarRecuperacaoSenha(email);

            logAuditoriaService.registrarLog(
                    "SENHA_RECU_INICIO",
                    0L,
                    email,
                    "Processo de recuperação de senha iniciado.");

            redirectAttributes.addFlashAttribute(
                    "sucesso",
                    "Um link para resetar sua senha foi enviado para o seu email.");

        } catch (IllegalArgumentException e) {

            // Não revelamos para o usuário se o e-mail existe
            redirectAttributes.addFlashAttribute(
                    "sucesso",
                    "Um link para resetar sua senha foi enviado (se o email existir em nosso sistema).");

            logAuditoriaService.registrarLog(
                    "SENHA_RECU_FALHA",
                    0L,
                    email,
                    "Email não encontrado ou erro de envio");

        } catch (Exception e) {

            redirectAttributes.addFlashAttribute(
                    "erro",
                    "Erro ao processar a solicitação");

            logAuditoriaService.registrarLog(
                    "SENHA_RECU_FALHA",
                    0L,
                    email,
                    "Erro inesperado");
        }

        return "redirect:/clientes/login";
    }

    /** 
     * @param token
     * @param model
     * @return String
     */
    @GetMapping("/reset-senha")
    public String mostrarFormularioResetSenha(@RequestParam("token") String token, Model model) {

        try {
            boolean tokenValido = clienteService.validarTokenRecuperacao(token);

            if (!tokenValido) {
                model.addAttribute("erro", "Token inválido ou expirado. Solicite a recuperação novamente.");

                logAuditoriaService.registrarLog(
                        "SENHA_RESET_TOKEN_FALHA",
                        0L,
                        "TOKEN_INVALIDO",
                        "Tentativa de uso de token inválido: " + token);

                return "cliente/login_cliente";
            }

            // Criamos o DTO para amarrar no formulário
            model.addAttribute("resetData", new SenhaResetDTO(token, null, null));
            model.addAttribute("tokenValido", true);

            return "cliente/reset_senha";

        } catch (Exception e) {

            model.addAttribute("erro", "Ocorreu um erro ao validar o token");

            logAuditoriaService.registrarLog(
                    "SENHA_RESET_TOKEN_FALHA",
                    0L,
                    "ERRO_VALIDACAO",
                    "Erro ao validar token");

            return "cliente/login_cliente";
        }
    }

    /** 
     * @param resetDTO
     * @param redirectAttributes
     * @return String
     */
    @PostMapping("/alterar-senha")
    public String alterarSenha(@ModelAttribute("resetData") SenhaResetDTO resetDTO,
            RedirectAttributes redirectAttributes) {

        String token = resetDTO.getToken();
        String novaSenha = resetDTO.getNovaSenha();
        String confirmarSenha = resetDTO.getConfirmarSenha();

        try {
            // Validações manuais tradicionais
            if (token == null || token.isEmpty()) {
                redirectAttributes.addFlashAttribute("erro", "Token ausente.");
                return "redirect:/clientes/login";
            }

            if (novaSenha == null || novaSenha.isEmpty() ||
                    confirmarSenha == null || confirmarSenha.isEmpty() ||
                    !novaSenha.equals(confirmarSenha)) {

                redirectAttributes.addFlashAttribute("erro", "As senhas não conferem ou dados incompletos.");

                // Preserva o token para reuso na tela
                redirectAttributes.addFlashAttribute(
                        "resetData",
                        new SenhaResetDTO(token, null, null));

                return "redirect:/clientes/reset-senha?token=" + token;
            }

            // Alteração real da senha
            String emailDoCliente = clienteService.alterarSenhaComToken(token, novaSenha);

            logAuditoriaService.registrarLog(
                    "SENHA_ALTERADA_SUCESSO",
                    0L,
                    emailDoCliente,
                    "Senha alterada com sucesso via token.");

            redirectAttributes.addFlashAttribute("sucesso", "Senha alterada com sucesso! Faça login.");

            return "redirect:/clientes/login";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute("erro", e.getMessage());

            return "redirect:/clientes/login";

        } catch (Exception e) {

            logAuditoriaService.registrarLog(
                    "SENHA_ALTERADA_FALHA",
                    0L,
                    "TOKEN: " + token,
                    "Erro inesperado");

            redirectAttributes.addFlashAttribute("erro", "Erro interno ao alterar a senha.");

            redirectAttributes.addFlashAttribute(
                    "resetData",
                    new SenhaResetDTO(token, null, null));

            return "redirect:/clientes/reset-senha?token=" + token;
        }
    }

}