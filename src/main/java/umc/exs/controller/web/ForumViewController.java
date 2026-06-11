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
import umc.exs.dto.request.cliente.NovoTopicoRequest;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.interactions.ForumService;

/**
 * Renderiza as páginas do fórum via Thymeleaf: listagem de tópicos, detalhe e criação de respostas.
 * Controla também a criação de novos tópicos e enriquece o model com dados do usuário autenticado.
 */
@Controller
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumViewController {

    private final ForumService forumService;
    private final ClienteRepository clienteRepo;

    // ── GET /forum — Lista de tópicos ─────────────────────────────────────────

    /**
     * Lista os tópicos do fórum de forma paginada, com filtros opcionais de busca por texto e categoria.
     * Prepara o model com os dados necessários para renderizar o template forum/lista.
     */
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
        model.addAttribute("novoTopico", new NovoTopicoRequest());

        preencherDadosUsuario(user, model);

        return "forum/lista";
    }

    // ── GET /forum/topicos/{id} — Detalhe do tópico ───────────────────────────

    /**
     * Exibe o detalhe de um tópico com suas respostas e as curtidas do usuário autenticado.
     * Incrementa o contador de visualizações a cada acesso.
     */
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
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        model.addAttribute("topico", topico);
        model.addAttribute("respostasLiked", respostasLiked);
        model.addAttribute("isAdmin", isAdmin);

        preencherDadosUsuario(user, model);

        return "forum/topico";
    }

    // ── POST /forum/topicos — Criar novo tópico ───────────────────────────────

    /**
     * Processa o formulário de criação de novo tópico, validando os campos obrigatórios.
     * Em caso de erro de validação, reexibe a lista; em caso de sucesso, redireciona para o tópico criado.
     */
    @PostMapping("/topicos")
    public String criarTopico(
            @Valid @ModelAttribute("novoTopico") NovoTopicoRequest dto,
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

    /**
     * Adiciona uma resposta ao tópico informado e redireciona de volta ao detalhe do tópico.
     * Rejeita respostas vazias e redireciona para login caso o usuário não esteja autenticado.
     */
    @PostMapping("/topicos/{id}/respostas")
    public String criarResposta(
            @PathVariable Long id,
            @RequestParam("conteudo") String conteudo,
            @AuthenticationPrincipal UserDetails user,
            RedirectAttributes ra) {

        if (user == null)
            return "redirect:/clientes/login";

        if (conteudo == null || conteudo.isBlank()) {
            ra.addFlashAttribute("erro", "A resposta não pode estar vazia.");
            return "redirect:/forum/topicos/" + id;
        }

        Long clienteId = resolverClienteId(user);
        forumService.criarResposta(id, conteudo, clienteId);
        ra.addFlashAttribute("sucesso", "Resposta publicada!");
        return "redirect:/forum/topicos/" + id;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Adiciona ao model os atributos clienteLogado, clienteId, clienteNome e isAdmin.
     * Utilizado por todos os métodos do controller para controle de permissões na view.
     */
    private void preencherDadosUsuario(UserDetails user, Model model) {
        if (user != null) {
            Long clienteId = resolverClienteId(user);
            String nomeUsuario = clienteRepo.findByEmail(user.getUsername())
                    .map(c -> c.getNome())
                    .orElse(user.getUsername());
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

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

    /**
     * Busca o ID do cliente no banco pelo e-mail do usuário autenticado.
     * Retorna null se o usuário não estiver autenticado ou não for encontrado.
     */
    private Long resolverClienteId(UserDetails user) {
        if (user == null)
            return null;
        return clienteRepo.findByEmail(user.getUsername())
                .map(c -> c.getId())
                .orElse(null);
    }
}
