package umc.exs.controller.prod;

import jakarta.servlet.http.HttpServletResponse;
import umc.exs.backstage.security.JwtUserDetailsService;
import umc.exs.backstage.security.JwtUtil;
import umc.exs.backstage.service.ClienteService;
import umc.exs.backstage.service.FieldValidation;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.dtos.auth.SignupDTO;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.EnderecoDTO;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClientController {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtUserDetailsService userDetailsService;

    // ============================================================
    // 🔧 COOKIES
    // ============================================================

    private void addTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 3600)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // ============================================================
    // 🔹 CADASTRO
    // ============================================================

    @GetMapping("/cadastro")
    public String mostrarCadastro(Model model) {
        if (!model.containsAttribute("cliente"))
            model.addAttribute("cliente", new ClienteDTO());
        return "cliente/cadastro_cliente";
    }

    @PostMapping("/cadastro")
    public String cadastrarCliente(
            @ModelAttribute SignupDTO signupDTO,
            @RequestParam(value = "termsAccepted", required = false) Boolean termsAccepted,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            Model model,
            HttpServletResponse response) {

        if (Boolean.FALSE.equals(termsAccepted) || Boolean.FALSE.equals(privacyAccepted)) {
            model.addAttribute("erro", "É necessário aceitar os termos e a política de privacidade.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        if (signupDTO == null || !FieldValidation.validarCampos(signupDTO) ||
                !FieldValidation.isValidEmail(signupDTO.getEmail())) {
            model.addAttribute("erro", "Dados inválidos.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        signupDTO.setEmail(FieldValidation.sanitizeEmail(signupDTO.getEmail()));
        signupDTO.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);
        if (salvo == null || salvo.getId() == null) {
            model.addAttribute("erro", "Erro ao cadastrar cliente.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        // Autenticar e criar cookie
        autenticarEAdicionarCookie(salvo.getEmail(), response);

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

        if (signupDTO == null || !FieldValidation.validarCampos(signupDTO) ||
                !FieldValidation.isValidEmail(signupDTO.getEmail())) {
            model.addAttribute("erro", "Dados inválidos.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        signupDTO.setEmail(FieldValidation.sanitizeEmail(signupDTO.getEmail()));
        signupDTO.setSenha(passwordEncoder.encode(signupDTO.getSenha()));

        ClienteDTO salvo = clienteService.salvarClienteCompleto(signupDTO, enderecoDTO, cartaoDTO);
        if (salvo == null || salvo.getId() == null) {
            model.addAttribute("erro", "Erro ao cadastrar cliente.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        autenticarEAdicionarCookie(salvo.getEmail(), response);

        return "redirect:/clientes/homepage";
    }

    // ==========================================================================
    // 🔹 LOGIN
    // ==========================================================================

    @GetMapping("/login")
    public String mostrarLogin(Model model) {
        if (!model.containsAttribute("cliente"))
            model.addAttribute("cliente", new ClienteDTO());
        return "cliente/login_cliente";
    }

    @PostMapping("/login")
    public String loginCliente(@ModelAttribute("cliente") ClienteDTO clienteDTO,
            Model model, HttpServletResponse response) {

        if (clienteDTO == null || clienteDTO.getEmail() == null || clienteDTO.getSenha() == null) {
            model.addAttribute("erro", "Dados inválidos.");
        
            return "cliente/login_cliente";
        }

        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorEmail(clienteDTO.getEmail());
        System.out.println("Tentativa de login para o email: " + clienteDTO.getEmail());

        if (clienteOpt.isEmpty()) {
            model.addAttribute("erro", "Cliente não encontrado.");
            return "cliente/login_cliente";
        }

        ClienteDTO cliente = clienteOpt.get();
        System.out.println("Cliente encontrado: " + cliente);
        System.out.println("Senha fornecida: " + clienteDTO.getSenha() + " Senha em hash: " + passwordEncoder.encode(clienteDTO.getSenha()));
        System.out.println("Senha armazenada (hash): " + cliente.getSenha());

        if (!passwordEncoder.matches(clienteDTO.getSenha(), cliente.getSenha())) {
            model.addAttribute("erro", "Senha incorreta.");
            return "cliente/login_cliente";
        }

        autenticarEAdicionarCookie(cliente.getEmail(), response);

        return "redirect:/clientes/homepage";
    }

    @PostMapping("/logout")
    public String logoutCliente(HttpServletResponse response) {
        clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }

    // ==========================================================
    // 🏠 HOMEPAGE / CARREGAMENTO DO CLIENTE (GET)
    // URL: /clientes/homepage
    // Objetivo: Carregar o ClienteDTO (com a List<EnderecoDTO> inicializada)
    // ==========================================================
    @GetMapping("/homepage")
    public String getHomepage(Principal principal, Model model) {
        // 1. Obter o identificador do usuário logado (usando o email como exemplo)
        String emailDoClienteLogado = principal.getName();

        // 2. Buscar o ClienteDTO completo
        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado. Por favor, faça login novamente."));

        System.out.println("Cliente carregado para homepage: " + clienteDTO);

        // 3. Adicionar o ClienteDTO ao modelo para renderização na view
        model.addAttribute("cliente", clienteDTO);

        return "cliente/homepage"; 
    }

    // ==========================================================
    // 💾 ATUALIZAR CLIENTE (POST)
    // URL: /clientes/atualizar
    // Objetivo: Receber o ClienteDTO atualizado e iniciar o processo de
    // persistência
    // ==========================================================
    @PostMapping("/atualizar")
    public String atualizarCliente(
            @ModelAttribute("cliente") ClienteDTO clienteAtualizadoDTO,
            Principal principal,
            RedirectAttributes redirectAttributes) {

                System.out.println("Dados recebidos para atualização: " + clienteAtualizadoDTO.getSenha());

        try {
            String emailDoClienteLogado = principal.getName();
            // Buscar o ID (alternativamente, você pode usar o ID do DTO, mas a busca pelo
            // email é mais segura)
            Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                    .map(ClienteDTO::getId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

            // Chama o método unificado do Service para atualizar campos simples e coleções.
            
            clienteService.atualizarClienteEAssociacoes(clienteId, clienteAtualizadoDTO, passwordEncoder);

            redirectAttributes.addFlashAttribute("sucesso", "Suas informações foram atualizadas com sucesso!");

        } catch (Exception e) {
            // Em caso de erro (ex: validação, erro no DB)
            // É comum adicionar o erro ao RedirectAttributes e logar
            redirectAttributes.addFlashAttribute("erro", "Erro ao atualizar informações: " + e.getMessage());
            // Log do erro
            System.err.println("Erro ao processar atualização do cliente: " + e.getMessage());
            e.printStackTrace();
        }

        return "redirect:/clientes/homepage"; // Redireciona de volta à homepage
    }

    // ==========================================================
    // 🗑️ DELEÇÃO DE ENDEREÇO
    // URL: /clientes/removerEndereco/{id}
    // Objetivo: Lidar com a exclusão de um endereço existente
    // ==========================================================
    @GetMapping("/removerEndereco/{id}")
    public String removerEndereco(@PathVariable("id") Long enderecoId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String emailDoClienteLogado = principal.getName();
            Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                    .map(ClienteDTO::getId)
                    .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

            clienteService.deletarEnderecoDoCliente(clienteId, enderecoId);

            redirectAttributes.addFlashAttribute("sucesso", "Endereço removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover endereço: " + e.getMessage());
        }

        return "redirect:/clientes/homepage";
    }

    private void autenticarEAdicionarCookie(String email, HttpServletResponse response) {
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            String token = jwtUtil.generateToken(email);
            addTokenCookie(response, token);

            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignored) {
        }
    }
}
