package umc.exs.controller.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

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
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.request.cliente.ClienteUpdateRequest;
import umc.exs.dto.request.cliente.LoginRequest;
import umc.exs.dto.request.cliente.SenhaResetRequest;
import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.dto.response.gamificacao.MeuPerfilGameResponse;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
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

    private final ClienteMapper clienteMapper;


    // Constantes para evitar duplicação de literais (Code Smells)
    private static final String ATTR_CLIENTE = "cliente";
    private static final String ATTR_SUCESSO = "sucesso";
    private static final String VIEW_CADASTRO = "cliente/cadastro_cliente";
    private static final String VIEW_LOGIN = "cliente/login_cliente";
    private static final String REDIRECT_LOGIN = "redirect:/clientes/login";
    private static final String REDIRECT_PERFIL = "redirect:/clientes/meu-perfil";
    private static final String REDIRECT_ENDERECOS = "redirect:/clientes/meu-perfil?aba=enderecos";

    // ============================================================
    // AUTENTICAÇÃO (CADASTRO / LOGIN / SAIR)
    // ============================================================

    @GetMapping("/novo-cadastro")
    public String exibirFormularioCadastro(HttpServletResponse response, Model model) {
        if (!model.containsAttribute(ATTR_CLIENTE)) {
            model.addAttribute(ATTR_CLIENTE, new SignupRequest());
        }
        jwtUtil.clearJwtCookie(response);
        return VIEW_CADASTRO;
    }

    @PostMapping("/novo-cadastro")
    public String registrarCliente(
            @Valid @ModelAttribute(ATTR_CLIENTE) SignupRequest SignupRequest,
            BindingResult result,
            @RequestParam String confirmPassword,
            Model model,
            HttpServletResponse response) {

        if (result.hasErrors()) {
            SignupRequest.setSenha("");
            SignupRequest.setConfirmPassword("");
            return VIEW_CADASTRO;
        }

        if (!SignupRequest.getSenha().equals(confirmPassword)) {
            result.rejectValue("senha", "error.senha", "As senhas não coincidem.");
            return VIEW_CADASTRO;
        }

        try {
            clienteService.salvarCliente(SignupRequest);
            return REDIRECT_LOGIN + "?cadastro=ok";
        } catch (Exception e) {
            model.addAttribute("erro", e.getMessage());
            return VIEW_CADASTRO;
        }
    }

    @GetMapping("/login")
    public String exibirLogin(Model model) {
        if (!model.containsAttribute("loginData")) {
            model.addAttribute("loginData", new LoginRequest());
        }
        return VIEW_LOGIN;
    }

    @PostMapping("/login")
    public String realizarLogin(
            @RequestParam String email,
            @RequestParam String senha,
            Model model,
            HttpServletResponse response) {

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            boolean isAdmin = userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN"));

            if (isAdmin) {
                if (!passwordEncoder.matches(senha, userDetails.getPassword())) {
                    model.addAttribute("erro", "E-mail ou senha inválidos.");
                    return VIEW_LOGIN;
                }
                String token = jwtUtil.generateToken(email);
                jwtUtil.addTokenCookie(response, token);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return "redirect:/admin/dashboard";
            }
        } catch (UsernameNotFoundException ignored) {
            // Tenta cliente abaixo
        }

        try {
            Cliente cliente = clienteService.autenticarCliente(email, senha);
            if (cliente.getId() != null) {
                Long safeId = Objects.requireNonNull(cliente.getId(),
                        "ID do cliente não pode ser nulo após autenticação");

                authHelper.authenticateAndSetCookie(cliente.getEmail(), safeId, response, "LOGIN_SUCESSO");
                return "redirect:/clientes/homepage";
            } else
                return REDIRECT_LOGIN;

        } catch (IllegalArgumentException e) {
            model.addAttribute("erro", e.getMessage());
            return VIEW_LOGIN;
        }
    }

    @GetMapping("/homepage")
    public String exibirHomepage(@AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return REDIRECT_LOGIN;

        Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));
        model.addAttribute(ATTR_CLIENTE, cliente);
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
            return REDIRECT_LOGIN;

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin)
            return "redirect:/admin/painel";

        Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Perfil não encontrado."));

        MeuPerfilGameResponse perfilGamificacao = gamificacaoService.obterMeuPerfil(user.getUsername());

        model.addAttribute(ATTR_CLIENTE, cliente);
        model.addAttribute("perfilGamificacao", perfilGamificacao);

        try {
            PontuacaoUsuario pontuacao = gamificacaoService.buscarPontuacaoPorEmail(user.getUsername());
            if (pontuacao != null && pontuacao.getUltimaAtualizacao() != null) {
                long diasSemXp = java.time.temporal.ChronoUnit.DAYS.between(
                        pontuacao.getUltimaAtualizacao(), LocalDateTime.now());

                if (diasSemXp > 30) {
                    String msg = diasSemXp >= 45 ? "Seu XP foi zerado por inatividade prolongada."
                            : "Você está perdendo XP por inatividade.";
                    model.addAttribute("aviso", msg);
                }
            }
        } catch (Exception e) {
            // Log do erro se necessário
        }

        return "cliente/homepage";
    }

    @GetMapping("/meu-perfil-json")
    @ResponseBody
    public ResponseEntity<ClientePerfilResponse> perfilJson(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(clienteMapper::toPerfilResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cancelamento/{pedidoId}")
    public String paginaCancelamento(@PathVariable Long pedidoId,
            @AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return REDIRECT_LOGIN;
        model.addAttribute("pedidoId", pedidoId);
        return "cliente/cancelamento";
    }

    @GetMapping("/minhas-vendas/{id}")
    public String paginaDetalheVenda(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails user, Model model) {
        if (user == null)
            return REDIRECT_LOGIN;
        model.addAttribute("livroId", id);
        return "cliente/minhas-vendas-detalhe";
    }

    @PostMapping("/foto-perfil")
    public String uploadFotoPerfil(@RequestParam("foto") MultipartFile foto,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.uploadFotoPerfilParaUsuarioLogado(user.getUsername(), foto);
        ra.addFlashAttribute(ATTR_SUCESSO, "Foto de perfil atualizada!");
        return REDIRECT_PERFIL;
    }

    @PostMapping("/atualizar")
    public String atualizarCliente(@ModelAttribute(ATTR_CLIENTE) ClienteUpdateRequest dto,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.atualizarDadosLogados(user.getUsername(), dto);
        ra.addFlashAttribute(ATTR_SUCESSO, "Informações atualizadas!");
        return REDIRECT_PERFIL;
    }

    @PostMapping("/deletar")
    public String deletarConta(@AuthenticationPrincipal UserDetails user, HttpServletResponse response,
            RedirectAttributes ra) {
        clienteService.deletarContaPropria(user.getUsername());
        jwtUtil.clearJwtCookie(response);
        SecurityContextHolder.clearContext();
        ra.addFlashAttribute(ATTR_SUCESSO, "Sua conta foi removida.");
        return "redirect:/";
    }

    @PostMapping("/enderecos/novo")
    public String cadastrarEndereco(@ModelAttribute("endereco") Endereco enderecoDTO,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        try {
            clienteService.adicionarEnderecoParaUsuarioLogado(user.getUsername(), enderecoDTO);
            ra.addFlashAttribute(ATTR_SUCESSO, "Endereço cadastrado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao salvar: " + e.getMessage());
        }
        return REDIRECT_ENDERECOS;
    }

    @PostMapping("/enderecos/editar")
    public String editarEndereco(@ModelAttribute Endereco enderecoDTO,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        try {
            Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
            clienteService.atualizarEnderecoDoCliente(cliente.getId(), enderecoDTO);
            ra.addFlashAttribute(ATTR_SUCESSO, "Endereço atualizado com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao atualizar: " + e.getMessage());
        }
        return REDIRECT_ENDERECOS;
    }

    @PostMapping("/enderecos/remover")
    public String removerEndereco(@RequestParam Long enderecoId,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        try {
            Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
            clienteService.deletarEnderecoDoCliente(cliente.getId(), enderecoId);
            ra.addFlashAttribute(ATTR_SUCESSO, "Endereço removido com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao remover: " + e.getMessage());
        }
        return REDIRECT_ENDERECOS;
    }

    @PostMapping("/enderecos/selecionar")
    public String selecionarEndereco(@RequestParam Long enderecoId,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        try {
            clienteService.selecionarEnderecoParaUsuarioLogado(user.getUsername(), enderecoId);
            ra.addFlashAttribute(ATTR_SUCESSO, "Endereço de entrega atualizado!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", "Erro ao selecionar: " + e.getMessage());
        }
        return REDIRECT_ENDERECOS;
    }

    // ============================================================
    // 🪙 CARTEIRA E FINANÇAS
    // ============================================================

    @GetMapping("/minhas-compras")
    public String exibirMinhasCompras(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return REDIRECT_LOGIN;
        // Histórico de compras está consolidado em Meu Perfil
        return REDIRECT_PERFIL;
    }

    @GetMapping("/lista-desejos")
    public String exibirListaDesejos(@AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return REDIRECT_LOGIN;
        return "cliente/lista_desejos";
    }

    @GetMapping("/carteira")
    public String exibirCarteira(@AuthenticationPrincipal UserDetails user, Model model) {
        Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername()).orElseThrow();
        List<Transacao> historico = clienteService.listarHistoricoTransacoes(cliente.getEmail());

        model.addAttribute(ATTR_CLIENTE, cliente);
        model.addAttribute("historico", historico);
        return "cliente/carteira";
    }

    @PostMapping("/comprar-tokens")
    public String comprarTokens(@RequestParam Double valor,
            @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        clienteService.adicionarTokensParaUsuarioLogado(user.getUsername(), valor);
        ra.addFlashAttribute(ATTR_SUCESSO, "Recarga solicitada com sucesso!");
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
            // Silencioso para segurança
        }
        ra.addFlashAttribute(ATTR_SUCESSO, "E-mail de recuperação enviado! Verifique sua caixa de entrada.");
        return REDIRECT_LOGIN;
    }

    @GetMapping("/reset-senha")
    public String mostrarFormularioResetSenha(@RequestParam String token, Model model, RedirectAttributes ra) {
        if (!clienteService.validarTokenRecuperacao(token)) {
            ra.addFlashAttribute("erro", "Link inválido ou expirado.");
            return REDIRECT_LOGIN;
        }
        model.addAttribute("resetData", new SenhaResetRequest(token, null, null));
        return "cliente/reset_senha";
    }

    @PostMapping("/alterar-senha-perfil")
    public String alterarSenhaPerfil(@RequestParam String senhaAtual, @RequestParam String novaSenha,
            @RequestParam String confirmarSenha, @AuthenticationPrincipal UserDetails user, RedirectAttributes ra) {
        try {
            clienteService.alterarSenhaLogado(user.getUsername(), senhaAtual, novaSenha, confirmarSenha);
            ra.addFlashAttribute(ATTR_SUCESSO, "Senha alterada com sucesso!");
        } catch (Exception e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return REDIRECT_PERFIL;
    }

    @PostMapping("/alterar-senha")
    public String alterarSenha(@ModelAttribute("resetData") SenhaResetRequest resetDTO, RedirectAttributes ra) {
        if (!resetDTO.getNovaSenha().equals(resetDTO.getConfirmarSenha())) {
            ra.addFlashAttribute("erro", "As senhas não conferem.");
            return "redirect:/clientes/reset-senha?token=" + resetDTO.getToken();
        }
        clienteService.alterarSenhaComToken(resetDTO.getToken(), resetDTO.getNovaSenha());
        ra.addFlashAttribute(ATTR_SUCESSO, "Senha alterada!");
        return REDIRECT_LOGIN;
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