package umc.exs.controller.web;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.auth.LoginDTO;
import umc.exs.DTOs.auth.SignupDTO;
import umc.exs.DTOs.gamificacao.MeuPerfilGamificacaoDTO;
import umc.exs.DTOs.user.ClienteDTO;
import umc.exs.DTOs.user.ClienteUpdateDTO;
import umc.exs.DTOs.user.EnderecoDTO;
import umc.exs.DTOs.user.SenhaResetDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.security.JwtUserDetailsService;
import umc.exs.security.JwtUtil;
import umc.exs.service.core.cliente.ClienteService;
import umc.exs.service.core.control.AuthHelper;
import umc.exs.service.gamificacao.GamificacaoService;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClientController {

    private final ClienteService clienteService;
    private final AuthHelper authHelper;
    private final JwtUtil jwtUtil;
    private final JwtUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final GamificacaoService gamificacaoService;

    // ============================================================
    // AUTENTICAÇÃO (CADASTRO / LOGIN / SAIR)
    // ============================================================

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

        if (result.hasErrors()) {
            signupDTO.setSenha("");
            signupDTO.setConfirmPassword("");
            return "cliente/cadastro_cliente";
        }

        if (!signupDTO.getSenha().equals(confirmPassword)) {
            result.rejectValue("senha", "error.senha", "As senhas não coincidem.");
            return "cliente/cadastro_cliente";
        }

        try {
            clienteService.salvarCliente(signupDTO);
            // Não faz login automático — o cliente deve verificar o e-mail primeiro
            return "redirect:/clientes/login?cadastro=ok";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            return "cliente/cadastro_cliente";
        }
    }

    @GetMapping("/login")
    public String exibirLogin(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginDTO());
        }
        return "cliente/login_cliente";
    }

    /**
     * Login unificado: detecta admin ou cliente pelo e-mail.
     * Admin (tabela admins) → loga direto, sem verificar email_verificado.
     * Cliente (tabela users) → verifica email_verificado antes de permitir login.
     */
    @PostMapping("/login")
    public String realizarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            Model model,
            HttpServletResponse response) {

        // 1. Tenta admin primeiro
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN"));

            if (isAdmin) {
                if (!passwordEncoder.matches(senha, userDetails.getPassword())) {
                    model.addAttribute("erro", "E-mail ou senha inválidos.");
                    return "cliente/login_cliente";
                }
                String token = jwtUtil.generateToken(email);
                jwtUtil.addTokenCookie(response, token);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return "redirect:/admin/dashboard";
            }
        } catch (UsernameNotFoundException ignored) {
            // e-mail não é admin — tenta como cliente abaixo
        }

        // 2. Tenta cliente (verifica email_verificado internamente)
        try {
            ClienteDTO cliente = clienteService.autenticarCliente(email, senha);
            authHelper.authenticateAndSetCookie(cliente.getEmail(), cliente.getId(), response, "LOGIN_SUCESSO");
            return "redirect:/clientes/homepage";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return "cliente/login_cliente";
        }
    }

    @GetMapping("/homepage")
    public String exibirHomepage(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return "redirect:/clientes/login";
        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));
        model.addAttribute("cliente", clienteDTO);
        return "cliente/homepage";
    }

    @GetMapping("/sair")
    public String deslogar(HttpServletResponse response, @AuthenticationPrincipal UserDetails user) {
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        return "redirect:/?logout=true";
    }

    // ============================================================
    // 🏠 PERFIL E CONTA
    // ============================================================

    @GetMapping("/meu-perfil")
    public String exibirPerfil(@AuthenticationPrincipal UserDetails user, Model model) {

        if (user == null)
            return "redirect:/clientes/login";

        ClienteDTO clienteDTO = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));

        MeuPerfilGamificacaoDTO perfilGamificacao = gamificacaoService.obterMeuPerfil(user.getUsername());

        model.addAttribute("cliente", clienteDTO);
        model.addAttribute("perfilGamificacao", perfilGamificacao);

        // 🔥 NOVA REGRA DE INATIVIDADE
        try {

            PontuacaoUsuario pontuacao = gamificacaoService
                    .buscarPontuacaoPorEmail(user.getUsername());

            if (pontuacao != null && pontuacao.getUltimaAtualizacao() != null) {

                LocalDateTime agora = LocalDateTime.now();

                long diasSemXp = java.time.temporal.ChronoUnit.DAYS.between(
                        pontuacao.getUltimaAtualizacao(), agora);

                if (diasSemXp > 30) {

                    if (diasSemXp >= 45) {

                        model.addAttribute("aviso",
                                "Seu XP foi zerado por inatividade prolongada.");

                    } else {

                        model.addAttribute("aviso",
                                "Você está perdendo XP por inatividade. Volte a interagir para evitar perda total.");
                    }

                    // 🔥 aplica penalidade
                    gamificacaoService.aplicarPenalidadeXpExpirada(user.getUsername());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "cliente/homepage";
    }

    @GetMapping("/meu-perfil-json")
    @ResponseBody
    public ResponseEntity<?> perfilJson(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/foto-perfil")
    public String uploadFotoPerfil(@RequestParam("foto") MultipartFile foto,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.uploadFotoPerfilParaUsuarioLogado(user.getUsername(), foto);
        ra.addFlashAttribute("sucesso", "Foto de perfil atualizada!");
        return "redirect:/clientes/meu-perfil";
    }

    @PostMapping("/atualizar")
    public String atualizarCliente(@ModelAttribute("cliente") ClienteUpdateDTO dto,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.atualizarDadosLogados(user.getUsername(), dto);
        ra.addFlashAttribute("sucesso", "Informações atualizadas!");
        return "redirect:/clientes/meu-perfil";
    }

    @PostMapping("/deletar")
    public String deletarConta(@AuthenticationPrincipal UserDetails user, HttpServletResponse response,
            RedirectAttributes ra) {
        clienteService.deletarContaPropria(user.getUsername());
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        ra.addFlashAttribute("sucesso", "Sua conta foi removida.");
        return "redirect:/";
    }

    @PostMapping("/enderecos/novo")
    public String cadastrarEndereco(
            @ModelAttribute("endereco") EnderecoDTO enderecoDTO,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {

        try {
            clienteService.adicionarEnderecoParaUsuarioLogado(user.getUsername(), enderecoDTO);
            ra.addFlashAttribute("sucesso", "Endereço cadastrado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }

        return "redirect:/clientes/meu-perfil?aba=enderecos";
    }

    @PostMapping("/enderecos/editar")
    public String editarEndereco(
            @ModelAttribute EnderecoDTO enderecoDTO,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {

        try {
            ClienteDTO cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
            clienteService.atualizarEnderecoDoCliente(cliente.getId(), enderecoDTO);
            ra.addFlashAttribute("sucesso", "Endereço atualizado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar: " + e.getMessage());
        }

        return "redirect:/clientes/meu-perfil?aba=enderecos";
    }

    @PostMapping("/enderecos/remover")
    public String removerEndereco(
            @RequestParam Long enderecoId,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {

        try {
            ClienteDTO cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
            clienteService.deletarEnderecoDoCliente(cliente.getId(), enderecoId);
            ra.addFlashAttribute("sucesso", "Endereço removido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover: " + e.getMessage());
        }

        return "redirect:/clientes/meu-perfil?aba=enderecos";
    }

    // ============================================================
    // 🪙 CARTEIRA E FINANÇAS
    // ============================================================

    @GetMapping("/minhas-compras")
    public String exibirMinhasCompras(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return "redirect:/clientes/login";
        // Histórico de compras está consolidado em Meu Perfil
        return "redirect:/clientes/meu-perfil";
    }

    @GetMapping("/lista-desejos")
    public String exibirListaDesejos(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return "redirect:/clientes/login";
        return "cliente/lista_desejos";
    }

    @GetMapping("/carteira")
    public String exibirCarteira(@AuthenticationPrincipal UserDetails user, Model model) {
        ClienteDTO cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
        List<Transacao> historico = clienteService.listarHistoricoTransacoes(cliente.getEmail());

        model.addAttribute("cliente", cliente);
        model.addAttribute("historico", historico);
        return "cliente/carteira";
    }

    @PostMapping("/comprar-tokens")
    public String comprarTokens(@RequestParam Double valor,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.adicionarTokensParaUsuarioLogado(user.getUsername(), valor);
        ra.addFlashAttribute("sucesso", "Recarga solicitada com sucesso!");
        return "redirect:/clientes/carteira";
    }

    // ============================================================
    // 🔑 RECUPERAÇÃO DE SENHA
    // ============================================================

    @GetMapping("/recuperar-senha")
    public String mostrarPaginaRecuperarSenha() {
        return "cliente/recuperar_senha";
    }

    @PostMapping("/recuperar-senha")
    public String iniciarRecuperacaoSenha(@RequestParam String email, RedirectAttributes ra) {
        try {
            clienteService.iniciarRecuperacaoSenha(email);
        } catch (Exception ignored) {
            // Sempre exibe a mesma mensagem para não expor se o e-mail existe ou não
        }
        ra.addFlashAttribute("sucesso", "E-mail de recuperação enviado! Verifique sua caixa de entrada.");
        return "redirect:/clientes/login";
    }

    @GetMapping("/reset-senha")
    public String mostrarFormularioResetSenha(@RequestParam String token, Model model, RedirectAttributes ra) {
        if (!clienteService.validarTokenRecuperacao(token)) {
            ra.addFlashAttribute("erro", "Link inválido ou expirado.");
            return "redirect:/clientes/login";
        }
        model.addAttribute("resetData", new SenhaResetDTO(token, null, null));
        return "cliente/reset_senha";
    }

    @PostMapping("/alterar-senha-perfil")
    public String alterarSenhaPerfil(
            @RequestParam String senhaAtual,
            @RequestParam String novaSenha,
            @RequestParam String confirmarSenha,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        try {
            clienteService.alterarSenhaLogado(user.getUsername(), senhaAtual, novaSenha, confirmarSenha);
            ra.addFlashAttribute("sucesso", "Senha alterada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/clientes/meu-perfil";
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@ModelAttribute("resetData") SenhaResetDTO resetDTO, RedirectAttributes ra) {
        if (!resetDTO.getNovaSenha().equals(resetDTO.getConfirmarSenha())) {
            ra.addFlashAttribute("erro", "As senhas não conferem.");
            return "redirect:/clientes/reset-senha?token=" + resetDTO.getToken();
        }
        clienteService.alterarSenhaComToken(resetDTO.getToken(), resetDTO.getNovaSenha());
        ra.addFlashAttribute("sucesso", "Senha alterada!");
        return "redirect:/clientes/login";
    }

    // ============================================================
    // 📄 PÁGINAS INFORMATIVAS
    // ============================================================

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
}