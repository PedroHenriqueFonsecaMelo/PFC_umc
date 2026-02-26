package umc.exs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/livros")
public class LivroViewController {

    // Rota para abrir o formulário de cadastro/venda
    @GetMapping("/vender")
    public String paginaVender() {
        return "venda_livro"; // Procura por venda_livro.html em /templates
    }

    // Rota para abrir a vitrine de compras
    @GetMapping("/vitrine")
    public String paginaVitrine() {
        return "vitrine_livros"; // Procura por vitrine_livros.html em /templates
    }
}