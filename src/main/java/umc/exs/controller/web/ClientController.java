package umc.exs.controller.web;

import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.DTOs.user.SenhaResetDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.AuthHelper;
import umc.exs.service.core.ClienteService;
import umc.exs.service.log.LogAuditoriaService;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClientController {

    /**
     * Exibe formulário de cadastro novo cliente.
     * Limpa cookie JWT anterior.
     * 
     * @param response HttpServletResponse para cookies
     * @param model    Model para SignupDTO
     */

    private final ClienteService clienteService;

    private final LogAuditoriaService logAuditoriaService;
    private final AuthHelper authHelper;
    private final JwtUtil jwtUtil;

    // ============================================================
    // 🔹 CADASTRO E LOGIN
    // ============================================================

    /**
     * Exibe form cadastro cliente novo.
     * Limpa JWT cookie, prepara SignupDTO.
     * 
     * @param response clear cookie
     * @param model    SignupDTO
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
     * Registra cliente básico novo.
     * Valida termos/senha match, salva ClienteService.
     * Autentica cookie, redirect perfil.
     * Trata bindingResult erros.
     */
    @PostMapping("/novo-cadastro")
    public String registrarCliente(

            @Valid @ModelAttribute("cliente") SignupDTO signupDTO,
            BindingResult result,
            @RequestParam(name = "confirmPassword") String confirmPassword,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(signupDTO.getTermsAccepted())
                || Boolean.FALSE.equals(signupDTO.getPrivacyAccepted())) {
            model.addAttribute("erro", "É necessário aceitar os termos e políticas de privacidade.");
            return "cliente/cadastro_cliente";
        }

        if (!signupDTO.getSenha().equals(confirmPassword)) {
            result.rejectValue("senha", "error.senha", "As senhas não coincidem.");
        }

        if (result.hasErrors())
            return "cliente/cadastro_cliente";

        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);
        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_SUCESSO");
        return "redirect:/clientes/meu-perfil";
    }

    /**
     * Registra cliente completo (end/ cartao).
     * Valida termos, salvaCompleto ClienteService.
     * Autentica cookie, redirect perfil.
     * @param signupDTO cliente, enderecoDTO, cartaoDTO
     */
    @PostMapping("/cadastro-completo")
    public String cadastrarClienteCompleto(

            @Valid @ModelAttribute("cliente") SignupDTO signupDTO,
            @ModelAttribute EnderecoDTO enderecoDTO,
            @ModelAttribute CartaoDTO cartaoDTO,
            @RequestParam String confirmPassword,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(signupDTO.getTermsAccepted())
                || Boolean.FALSE.equals(signupDTO.getPrivacyAccepted())) {
            model.addAttribute("erro", "Aceite os termos para continuar.");
            return "cliente/cadastro_cliente";
        }

        ClienteDTO salvo = clienteService.salvarClienteCompleto(signupDTO, enderecoDTO, cartaoDTO);
        authHelper.authenticateAndSetCookie(salvo.getEmail(), salvo.getId(), response, "CADASTRO_COMPLETO_SUCESSO");
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
    public String realizarLogin(@Valid @ModelAttribute("loginData") LoginDTO loginDTO, BindingResult result,
            Model model, HttpServletResponse response) {
        if (result.hasErrors())
            return "cliente/login_cliente";

        Optional<ClienteDTO> clienteOpt = clienteService.autenticarCliente(loginDTO.getEmail(), loginDTO.getSenha());
        if (clienteOpt.isEmpty()) {
            logAuditoriaService.registrarLog("LOGIN_FALHA", 0L, loginDTO.getEmail(), "Credenciais inválidas.");
            model.addAttribute("erro", "E-mail ou senha inválidos.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();
        authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");
        return "redirect:/";
    }

    @GetMapping("/sair")
    public String deslogar(HttpServletResponse response, @AuthenticationPrincipal UserDetails user) {
        if (user != null) {
            Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElse(0L);
            logAuditoriaService.registrarLog("LOGOUT_SUCESSO", id, user.getUsername(), "Sessão encerrada.");
        }
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // ==========================================================
    // 🏠 PERFIL E ATUALIZAÇÃO
    // ==========================================================

    @GetMapping("/meu-perfil")
    public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return "redirect:/clientes/login";

        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));

        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

    /**
     * Retorna os dados do cliente logado como JSON.
     * Usado pelo frontend para exibir saldo e nome sem recarregar a página.
     */
    @GetMapping("/meu-perfil-json")
    @ResponseBody
    public ResponseEntity<?> perfilJson(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("Não autenticado.");
        return clienteService.buscarClientePorEmail(user.getUsername())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body("Cliente não encontrado."));
    }

    /**
     * Página de compras do cliente (pendentes + concluídas).
     */
    @GetMapping("/minhas-compras")
    public String minhasCompras(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return "redirect:/clientes/login";
        return "cliente/minhas-compras";
    }

    @PostMapping("/foto-perfil")
    public String uploadFotoPerfil(@RequestParam("foto") MultipartFile foto,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        clienteService.uploadFotoPerfil(id, foto);
        ra.addFlashAttribute("sucesso", "Foto de perfil atualizada!");
        return "redirect:/clientes/meu-perfil";
    }

    @PostMapping("/atualizar")
    public String atualizarCliente(@ModelAttribute("cliente") ClienteDTO clienteDTO,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        clienteService.atualizarClienteEAssociacoes(id, clienteDTO);
        ra.addFlashAttribute("sucesso", "Informações atualizadas com sucesso!");
        return "redirect:/clientes/meu-perfil";
    }

    @PostMapping("/deletar")
    public String deletarConta(@AuthenticationPrincipal UserDetails user, HttpServletResponse response,
            RedirectAttributes ra) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        clienteService.deletarClientePorId(id);
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        ra.addFlashAttribute("sucesso", "Sua conta foi removida.");
        return "redirect:/";
    }

    // ==========================================================
    // 🪙 CARTEIRA E TRANSAÇÕES
    // ==========================================================

    @GetMapping("/carteira")
    public String exibirCarteira(@AuthenticationPrincipal UserDetails user, Model model) {
        ClienteDTO cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
        List<Transacao> historico = clienteService.listarHistoricoTransacoes(cliente.getId());

        model.addAttribute("cliente", cliente);
        model.addAttribute("historico", historico);
        return "cliente/carteira";
    }

    @PostMapping("/comprar-tokens")
    public String comprarTokens(@RequestParam Double valor, @RequestParam String metodo,
            @RequestParam(required = false) String numCartao,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        clienteService.adicionarTokens(id, valor, metodo, numCartao);
        ra.addFlashAttribute("sucesso", "Tokens adicionados!");
        return "redirect:/clientes/carteira";
    }

    // ==========================================================
    // 🔑 RECUPERAÇÃO DE SENHA
    // ==========================================================

    @GetMapping("/recuperar-senha")
    public String mostrarPaginaRecuperarSenha() {
        return "cliente/recuperar_senha";
    }

    @PostMapping("/recuperar-senha")
    public String iniciarRecuperacaoSenha(@RequestParam("email") String email, RedirectAttributes ra) {
        try {
            clienteService.iniciarRecuperacaoSenha(email);
            logAuditoriaService.registrarLog("SENHA_RECU_INICIO", 0L, email, "Processo iniciado.");
            ra.addFlashAttribute("sucesso", "Link enviado para o seu e-mail.");
        } catch (Exception e) {
            ra.addFlashAttribute("sucesso", "Se o e-mail existir, um link foi enviado.");
        }
        return "redirect:/clientes/login";
    }

    @GetMapping("/reset-senha")
    public String mostrarFormularioResetSenha(@RequestParam("token") String token, Model model) {
        if (!clienteService.validarTokenRecuperacao(token)) {
            model.addAttribute("erro", "Token inválido ou expirado.");
            return "cliente/login_cliente";
        }
        model.addAttribute("resetData", new SenhaResetDTO(token, null, null));
        model.addAttribute("tokenValido", true);
        return "cliente/reset_senha";
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@ModelAttribute("resetData") SenhaResetDTO resetDTO, RedirectAttributes ra) {
        if (!resetDTO.getNovaSenha().equals(resetDTO.getConfirmarSenha())) {
            ra.addFlashAttribute("erro", "As senhas não conferem.");
            return "redirect:/clientes/reset-senha?token=" + resetDTO.getToken();
        }
        clienteService.alterarSenhaComToken(resetDTO.getToken(), resetDTO.getNovaSenha());
        ra.addFlashAttribute("sucesso", "Senha alterada com sucesso!");
        return "redirect:/clientes/login";
    }

    // --- PÁGINAS ESTÁTICAS/LEGAIS ---
    @GetMapping("/termo")
    public String mostrarTermo() {
        return "cliente/Termo";
    }

    @GetMapping("/politica")
    public String mostrarPolitica() {
        return "cliente/Politica";
    }

    @GetMapping("/sobre")
    public String mostrarSobre() {
        return "cliente/Sobre";
    }

    // --- Fundos

    @GetMapping("/fundos")
    public String adicionarFundos(@AuthenticationPrincipal UserDetails user, Model model) {
        ClienteDTO cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
        model.addAttribute("cliente", cliente);
        return "cliente/carteira";
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller web cliente completo (cadastro/login/perfil/carteira/senha).
 * Gerencia Thymeleaf views, JWT cookies, validações, auditoria logs.
 * Rota /clientes/** autenticação UserDetails.
 */
