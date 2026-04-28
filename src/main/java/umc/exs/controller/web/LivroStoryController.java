package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/livros")
public class LivroStoryController {

    /**
     * Página para ver a história do livro, resenhas e pontuações.
     * O ISBN é passado para o template para que o JS saiba o que buscar.
     */
    @GetMapping("/{isbn}/historia")
    public String paginaHistoriaLivro(@PathVariable String isbn, Model model) {
        // Passamos o isbn para o HTML para usá-lo em tags meta ou scripts
        model.addAttribute("isbn", isbn);
        model.addAttribute("unificado", false);

        return "produto/historia_livro";
    }

    @GetMapping("/{isbn}/unificado")
    public String paginaHistoriaLivroUnificado(@PathVariable String isbn, Model model) {

        model.addAttribute("isbn", isbn);
        model.addAttribute("unificado", true);

        return "produto/historia_livro";
    }

    /**
     * Rota de teste para validar o mapeamento da raiz de templates
     */
    @GetMapping("/teste")
    public String teste() {
        // Retorna o arquivo em: src/main/resources/templates/historia_livro.html
        return "produto/historia_livro";
    }
}