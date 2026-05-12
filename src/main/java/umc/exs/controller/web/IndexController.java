package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    @GetMapping({ "/", "/index", "/home" })
    public String index(Model model) {
        model.addAttribute("statLivros",   livroRepository.countByAprovadoTrue());
        model.addAttribute("statLeitores", clienteRepository.count());
        model.addAttribute("statTrocas",   pedidoRepository.count());
        return "index";
    }

    /** Mantida para não quebrar links existentes — redireciona para a tela unificada. */
    @GetMapping("/entrar")
    public String entrar() {
        return "redirect:/clientes/login";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

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
