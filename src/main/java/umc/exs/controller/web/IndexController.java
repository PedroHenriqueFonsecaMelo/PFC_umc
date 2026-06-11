package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;

/**
 * Gerencia as páginas públicas da plataforma: home, shop, login e ranking.
 * Não exige autenticação — todas as rotas são acessíveis por qualquer visitante.
 */
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    /**
     * Exibe a homepage com estatísticas de livros aprovados, leitores cadastrados e trocas realizadas.
     * Os dados são carregados diretamente do banco a cada requisição.
     */
    @GetMapping({ "/", "/index", "/home" })
    public String index(Model model) {
        model.addAttribute("statLivros", livroRepository.countByAprovadoTrue());
        model.addAttribute("statLeitores", clienteRepository.count());
        model.addAttribute("statTrocas", pedidoRepository.count());
        return "index";
    }

    /**
     * Mantida para não quebrar links existentes — redireciona para a tela
     * unificada.
     */
    @GetMapping("/entrar")
    public String entrar() {
        return "redirect:/clientes/login";
    }

    /** Exibe a página de vitrine de livros disponíveis para compra. */
    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    /** Exibe a página de login para autenticação do cliente. */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /** Exibe a página administrativa; o controle de acesso é gerenciado pelo SecurityConfig. */
    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }

    /** Página pública de ranking geral — acessível sem login. */
    @GetMapping("/ranking")
    public String ranking() {
        return "ranking";
    }
}
