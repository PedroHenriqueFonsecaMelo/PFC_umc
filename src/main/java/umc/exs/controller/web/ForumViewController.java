package umc.exs.controller.web;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.forum.NovaRespostaDTO;
import umc.exs.DTOs.forum.NovoTopicoDTO;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.ForumService;

@Controller
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumViewController {

    private final ForumService forumService;
    private final ClienteRepository clienteRepo;

    // ── GET /forum — Lista de tópicos ─────────────────────────────────────────

    @GetMapping
    public String listarTopicos(
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) CategoriaForum categoria,
            @RequestParam(defaultValue = "0") int pagina,
            @AuthenticationPrincipal UserDetails user,
            Model model) {

        PageRequest pageable = PageRequest.of(pagina, 10,
                Sort.by(Sort.Direction.DESC, "dataCriacao"));
        Page<TopicoForum> topicos = forumService.listarTopicos(busca, categoria, pageable);

        model.addAttribute("topicos", topicos);
        model.addAttribute("categorias", CategoriaForum.values());
        model.addAttribute("categoriaSelecionada", categoria);
        model.addAttribute("busca", busca != null ? busca : "");
        model.addAttribute("pagina", pagina);
        model.addAttribute("novoTopico", new NovoTopicoDTO());

        preencherDadosUsuario(user, model);

        return "forum/lista";
    }

    // ── GET /forum/topicos/{id} — Detalhe do tópico ───────────────────────────

    @GetMapping("/topicos/{id}")
    public String verTopico(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user,
            Model model) {

        TopicoForum topico = forumService.buscarTopicoPorId(id);
        forumService.incrementarVisualizacoes(id);

        Long clienteId = resolverClienteId(user);
        Set<Long> respostasLiked = forumService.getRespostasLikedByUser(id, clienteId);
        boolean isAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN"));

        model.addAttribute("topico", topico);
        model.addAttribute("novaResposta", new NovaRespostaDTO());
        model.addAttribute("respostasLiked", respostasLiked);
        model.addAttribute("isAdmin", isAdmin);

        preencherDadosUsuario(user, model);

        return "forum/topico";
    }

    // ── POST /forum/topicos — Criar novo tópico ───────────────────────────────

    @PostMapping("/topicos")
    public String criarTopico(
            @Valid @ModelAttribute("novoTopico") NovoTopicoDTO dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) CategoriaForum categoria,
            Model model,
            RedirectAttributes ra) {

        if (user == null)
            return "redirect:/clientes/login";

        if (result.hasErrors()) {
            Page<TopicoForum> topicos = forumService.listarTopicos(busca, categoria,
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataCriacao")));
            model.addAttribute("topicos", topicos);
            model.addAttribute("categorias", CategoriaForum.values());
            model.addAttribute("busca", busca != null ? busca : "");
            model.addAttribute("erroCriarTopico", true);
            preencherDadosUsuario(user, model);
            return "forum/lista";
        }

        Long clienteId = resolverClienteId(user);
        TopicoForum topico = forumService.criarTopico(dto, clienteId);
        ra.addFlashAttribute("sucesso", "Tópico criado com sucesso!");
        return "redirect:/forum/topicos/" + topico.getId();
    }

    // ── POST /forum/topicos/{id}/respostas — Responder tópico ────────────────

    @PostMapping("/topicos/{id}/respostas")
    public String criarResposta(
            @PathVariable Long id,
            @Valid @ModelAttribute("novaResposta") NovaRespostaDTO dto,
            BindingResult result,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {

        if (user == null)
            return "redirect:/clientes/login";

        if (result.hasErrors()) {
            ra.addFlashAttribute("erro", "A resposta não pode estar vazia.");
            return "redirect:/forum/topicos/" + id;
        }

        Long clienteId = resolverClienteId(user);
        forumService.criarResposta(id, dto, clienteId);
        ra.addFlashAttribute("sucesso", "Resposta publicada!");
        return "redirect:/forum/topicos/" + id;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void preencherDadosUsuario(UserDetails user, Model model) {
        if (user != null) {
            Long clienteId = resolverClienteId(user);
            String nomeUsuario = clienteRepo.findByEmail(user.getUsername())
                    .map(c -> c.getNome())
                    .orElse(user.getUsername());
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ADMIN"));

            model.addAttribute("clienteLogado", true);
            model.addAttribute("clienteId", clienteId);
            model.addAttribute("clienteNome", nomeUsuario);
            model.addAttribute("isAdmin", isAdmin);
        } else {
            model.addAttribute("clienteLogado", false);
            model.addAttribute("clienteId", null);
            model.addAttribute("clienteNome", null);
            model.addAttribute("isAdmin", false);
        }
    }

    private Long resolverClienteId(UserDetails user) {
        if (user == null)
            return null;
        return clienteRepo.findByEmail(user.getUsername())
                .map(c -> c.getId())
                .orElse(null);
    }
}
