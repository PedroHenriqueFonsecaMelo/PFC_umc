package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/livros")
public class LivroViewController {

    // Rota para abrir o formulário de cadastro/venda
    @GetMapping("/vender")
    public String paginaVender() {
        return "produto/venda_livro";
    }

    // Rota para abrir a vitrine de compras
    @GetMapping("/vitrine")
    public String paginaVitrine() {
        return "produto/vitrine_livros";
    }
}