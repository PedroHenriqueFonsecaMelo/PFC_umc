package umc.exs.controller.prod;

import java.util.Optional;

import org.springframework.beans.BeanUtils;
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
import org.springframework.web.bind.annotation.*;

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
                .secure(false) // alterar para true em produção
                .path("/")
                .maxAge(7 * 24 * 3600)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearJwtCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    // ============================================================
    // 🔹 VERIFICAÇÃO DE PERMISSÃO
    // ============================================================

    private boolean isAdminByEmail(String email) {
        if (email == null)
            return false;
        try {
            UserDetails ud = userDetailsService.loadUserByUsername(email);
            return ud.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        } catch (UsernameNotFoundException e) {
            return false;
        }
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

        System.out.println("Signup: " + signupDTO.toString());

        ClienteDTO salvo = clienteService.salvarCliente(signupDTO);
        if (salvo == null || salvo.getId() == null) {
            model.addAttribute("erro", "Erro ao cadastrar cliente.");
            model.addAttribute("cliente", signupDTO);
            return "cliente/cadastro_cliente";
        }

        try {
            String token = jwtUtil.generateToken(salvo.getEmail());
            addTokenCookie(response, token);

            UserDetails ud = userDetailsService.loadUserByUsername(salvo.getEmail());
            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (UsernameNotFoundException ignored) {
        }

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

        return "redirect:/clientes/homepage";
    }

    // ============================================================
    // 🔹 LOGIN
    // ============================================================

    @GetMapping("/login")
    public String loginForm() {
        return "cliente/cliente_login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String senha,
            Model model,
            HttpServletResponse response) {

        try {
            String sanitizedEmail = FieldValidation.sanitizeEmail(email);
            if (sanitizedEmail == null || !FieldValidation.isValidEmail(sanitizedEmail)) {
                model.addAttribute("erro", "Email inválido.");
                return "cliente/cliente_login";
            }

            UserDetails ud = userDetailsService.loadUserByUsername(sanitizedEmail);
            if (ud == null || !passwordEncoder.matches(senha, ud.getPassword())) {
                model.addAttribute("erro", "Credenciais inválidas.");
                return "cliente/cliente_login";
            }

            String token = jwtUtil.generateToken(sanitizedEmail);
            addTokenCookie(response, token);

            Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Optional<ClienteDTO> cliente = clienteService.buscarClientePorEmail(sanitizedEmail);
            model.addAttribute("cliente", cliente.orElse(null));

            return "redirect:/clientes/homepage";

        } catch (UsernameNotFoundException e) {
            model.addAttribute("erro", "Usuário não encontrado.");
            return "cliente/cliente_login";
        } catch (Exception e) {
            model.addAttribute("erro", "Erro ao realizar login.");
            return "cliente/cliente_login";
        }
    }

    @PostMapping("/atualizar")
    public String updateClient(@ModelAttribute ClienteDTO clienteDTO, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "cliente/acesso_negado";
        }

        System.out.println("🔹 cliente: " + clienteDTO);

        String email = auth.getName();
        boolean isAdmin = isAdminByEmail(email);

        Optional<ClienteDTO> existingClientOpt = clienteService.buscarClientePorEmail(email);
        if (existingClientOpt.isEmpty()) {
            return "cliente/cliente_nao_encontrado";
        }

        ClienteDTO existente = existingClientOpt.get();

        if (!isAdmin && !existente.getEmail().equals(email)) {
            return "cliente/acesso_negado";
        }

        // Copia os campos básicos (nome, email, etc.)
        ClienteService.copyNonNullProperties(clienteDTO, existente);

        // Mantém a senha antiga caso não tenha sido alterada
        if (clienteDTO.getSenha() == null || clienteDTO.getSenha().isBlank()) {
            existente.setSenha(existente.getSenha());
        }

        // =====================================================
        // 🔹 TRATAMENTO DOS ENDEREÇOS
        // =====================================================
        if (clienteDTO.getEnderecos() != null) {
            // 1️⃣ Remove endereços que não estão mais no formulário
            existente.getEnderecos().removeIf(
                    e -> clienteDTO.getEnderecos().stream()
                            .noneMatch(novo -> novo.getId() != null && novo.getId().equals(e.getId())));

            // 2️⃣ Atualiza ou adiciona novos endereços
            for (var novo : clienteDTO.getEnderecos()) {
                if (novo.getId() == null) {
                    // Novo endereço
                    existente.getEnderecos().add(novo);
                } else {
                    // Atualiza campos de um endereço existente
                    existente.getEnderecos().stream()
                            .filter(e -> e.getId().equals(novo.getId()))
                            .findFirst()
                            .ifPresent(e -> {
                                e.setRua(novo.getRua());
                                e.setNumero(novo.getNumero());
                                e.setBairro(novo.getBairro());
                                e.setCidade(novo.getCidade());
                                e.setEstado(novo.getEstado());
                                e.setCep(novo.getCep());
                                e.setPais(novo.getPais());
                                e.setComplemento(novo.getComplemento());
                                e.setTipoResidencia(novo.getTipoResidencia());
                            });
                }
            }
        }

        // =====================================================
        // 🔹 SALVAR E RETORNAR
        // =====================================================
        ClienteDTO atualizado = clienteService.salvarCliente(existente);
        
        model.addAttribute("cliente", atualizado);
        model.addAttribute("mensagem", "Cliente atualizado com sucesso.");

        return "redirect:/clientes/homepage";
    }

    // ============================================================
    // 🔹 LOGOUT
    // ============================================================

    @GetMapping("/logout")
    public String logout(HttpServletResponse response, Model model) {
        clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        model.addAttribute("mensagem", "Você saiu com sucesso.");
        return "cliente/cliente_login";
    }

    // ============================================================
    // 🔹 HOMEPAGE AUTENTICADA
    // ============================================================

    @GetMapping("/homepage")
    public String homepage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "cliente/acesso_negado";
        }

        String email = auth.getName();
        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorEmail(email);
        if (clienteOpt.isEmpty()) {
            return "cliente/cliente_nao_encontrado";
        }
        System.out.println("🔹 Cliente na homepage: " + clienteOpt.get());
        model.addAttribute("cliente", clienteOpt.get());
        return "cliente/homepage";
    }

    // ============================================================
    // 🔹 DETALHES E REMOÇÃO
    // ============================================================

    @GetMapping("/{id}")
    public String detalhesCliente(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return "cliente/acesso_negado";

        String email = auth.getName();
        boolean isAdmin = isAdminByEmail(email);

        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorId(id);
        if (clienteOpt.isEmpty())
            return "cliente/cliente_nao_encontrado";

        if (!isAdmin && !clienteOpt.get().getEmail().equals(email))
            return "cliente/acesso_negado";

        model.addAttribute("cliente", clienteOpt.get());
        return "cliente/detalhes_cliente";
    }

    @GetMapping("/{id}/remover")
    public String mostrarConfirmacaoRemocao(@PathVariable Long id, Model model) {
        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorId(id);
        if (clienteOpt.isEmpty())
            return "cliente/cliente_nao_encontrado";

        model.addAttribute("cliente", clienteOpt.get());
        return "cliente/remover_cliente";
    }

    @PostMapping("/{id}/remover")
    public String removerCliente(@PathVariable Long id, HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return "cliente/acesso_negado";

        String email = auth.getName();
        boolean isAdmin = isAdminByEmail(email);

        Optional<ClienteDTO> clienteOpt = clienteService.buscarClientePorId(id);
        if (clienteOpt.isEmpty())
            return "cliente/cliente_nao_encontrado";

        if (!isAdmin && !clienteOpt.get().getEmail().equals(email))
            return "cliente/acesso_negado";

        clienteRepository.deleteById(id);

        if (clienteOpt.get().getEmail().equals(email)) {
            clearJwtCookie(response);
            SecurityContextHolder.clearContext();
        }

        return "cliente/cliente_removido";
    }
}
