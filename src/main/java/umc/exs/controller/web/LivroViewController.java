package umc.exs.controller.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller Thymeleaf para o módulo de livros.
 *
 * Rotas públicas  : /livros/vitrine, /livros/{id}
 * Rotas autenticadas: /livros/estante, /livros/checkout (ver SecurityConfig)
 *
 * NOTA: Spring MVC prioriza rotas literais (/vender, /vitrine, /estante,
 * /checkout) antes da rota com variável de caminho (/{id}), eliminando
 * qualquer ambiguidade de mapeamento.
 */
@Controller
@RequestMapping("/livros")
public class LivroViewController {

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
     */
    @GetMapping("/checkout")
    public String paginaCheckout() {
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
