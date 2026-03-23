package umc.exs.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    /**
     * Página inicial/index/home do site.
     * Template index.html
     * 
     * @return view index
     */
    @GetMapping({ "/", "/index", "/home" })
    public String index() {
        return "index";
    }

    /**
     * Página loja/produtos.
     * Lista livros vitrine.
     * 
     * @return view shop
     */
    @GetMapping("/shop")
    public String shop() {
        return "shop";
    }

    /**
     * Página login genérica.
     * Redirect cliente/admin conforme auth.
     * 
     * @return view login
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * Página painel admin (não autenticada).
     * 
     * @return view admin
     */
    @GetMapping("/admin")
    public String adminPage() {
        return "admin";
    }
}

/**
 * DESCRIÇÃO DO ARQUIVO:
 * Controller rotas raiz site (home/shop/login/admin).
 * Render templates Thymeleaf sem lógica business.
 */
