package umc.exs.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.cliente.ClienteService;

/**
 * Controller Thymeleaf para o módulo de livros.
 *
 * Rotas públicas : /livros/vitrine, /livros/{id}
 * Rotas autenticadas: /livros/estante, /livros/checkout (ver SecurityConfig)
 *
 * NOTA: Spring MVC prioriza rotas literais (/vender, /vitrine, /estante,
 * /checkout) antes da rota com variável de caminho (/{id}), eliminando
 * qualquer ambiguidade de mapeamento.
 */
@Controller
@RequestMapping("/livros")
@RequiredArgsConstructor
public class LivroViewController {

    private final ClienteService clienteService;

    /** Formulário de cadastro para venda de livro. */
    @GetMapping("/vender")
    public String paginaVender(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("clienteLogado", user != null);
        return "produto/venda_livro";
    }

    /** Vitrine pública de livros disponíveis. */
    @GetMapping("/vitrine")
    public String paginaVitrine() {
        return "produto/vitrine_livros";
    }

    /**
     * Página pública de detalhes de um livro.
     * Os dados são carregados via JS em GET /api/livros/{id}.
     */
    @GetMapping("/{id}")
    public String paginaLivro(@PathVariable Long id) {
        return "produto/livro_detalhe";
    }

    /**
     * Página da estante: lista todos os livros salvos (localStorage).
     * Exige autenticação — bloqueada no SecurityConfig antes da regra /livros/**.
     */
    @GetMapping("/estante")
    public String paginaEstante() {
        return "produto/estante";
    }

    /**
     * Página de checkout: revisão e confirmação da compra selecionada.
     * Exige autenticação — bloqueada no SecurityConfig antes da regra /livros/**.
     * Usuários sem endereço cadastrado são redirecionados para o perfil.
     */
    @GetMapping("/checkout")
    public String paginaCheckout(@AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {
        Cliente cliente = clienteService.buscarClientePorEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado."));

        if (cliente.getEnderecos() == null || cliente.getEnderecos().isEmpty()) {
            ra.addFlashAttribute("aviso",
                    "Cadastre um endereço de entrega antes de realizar sua compra.");
            return "redirect:/clientes/meu-perfil";
        }

        return "produto/checkout";
    }

    /**
     * Página de confirmação exibida após uma compra bem-sucedida.
     * Os dados do pedido são lidos via sessionStorage no frontend.
     */
    @GetMapping("/pedido-confirmado")
    public String paginaPedidoConfirmado() {
        return "produto/pedido_confirmado";
    }
}
