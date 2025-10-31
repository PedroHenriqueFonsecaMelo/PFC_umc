package umc.exs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping({"/", "/index", "/home"})
    public String index() {
        return "index"; // resolve to src/main/resources/templates/index.html (Thymeleaf or your view engine)
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
}