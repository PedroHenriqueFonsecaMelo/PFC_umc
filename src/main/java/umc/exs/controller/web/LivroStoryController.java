package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/livros")
public class LivroStoryController {

    /**
     * Page to view book story, reviews and scores
     * Example: /livros/9780747532743/historia
     */
    @GetMapping("/{isbn}/historia")
    public String paginaHistoriaLivro(@PathVariable String isbn) {
        return "produto/historia_livro";
    }

    @GetMapping("/teste")
    public String teste() {
        return "historia_livro";
    }
}
