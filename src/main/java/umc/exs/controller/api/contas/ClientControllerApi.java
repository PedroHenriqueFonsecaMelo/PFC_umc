package umc.exs.controller.api.contas;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.mapper.ClienteMapper;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.dto.response.cliente.ClientePerfilResponse;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.livros.MinhasVendasService;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientControllerApi {

    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado.";

    private final ClienteService clienteService;
    private final MinhasVendasService minhasVendasService;

    private final ClienteMapper clienteMapper;

    /** Retorna dados do perfil para atualizar saldo/nome via JS */
    @GetMapping("/meu-perfil")
    public ResponseEntity<ClientePerfilResponse> getPerfilJson(
            @AuthenticationPrincipal UserDetails user) {

        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(clienteMapper::toPerfilResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Retorna histórico de transações em JSON */
    @GetMapping("/transacoes")
    public ResponseEntity<List<Transacao>> getHistorico(
            @AuthenticationPrincipal UserDetails user) {

        Long id = clienteService.buscarClientePorEmail(user.getUsername())
                .map(Cliente::getId)
                .orElseThrow();

        return ResponseEntity.ok(
                clienteService.listarHistoricoTransacoes(id));
    }

    /**
     * Endpoint para verificar se o e-mail já existe (validação em tempo real no
     * form)
     */
    @GetMapping("/verificar-email")
    public ResponseEntity<Boolean> verificarEmail(@RequestParam String email) {
        return ResponseEntity.ok(clienteService.buscarClientePorEmail(email).isPresent());
    }

    /**
     * Retorna os dados do cliente logado como JSON.
     * Usado pelo frontend para exibir saldo e nome sem recarregar a página.
     */
    @GetMapping("/meu-perfil-json")
    public ResponseEntity<ClientePerfilResponse> perfilJson(@AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(clienteMapper::toPerfilResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    /** Retorna o endereço selecionado para entrega do cliente logado */
    @GetMapping("/endereco-selecionado")
    public ResponseEntity<Endereco> getEnderecoSelecionado(
            @AuthenticationPrincipal UserDetails user) {

        return clienteService.buscarClientePorEmail(user.getUsername())
                .flatMap(c -> c.getEnderecos().stream()
                        .filter(e -> e.getId() != null &&
                                e.getId().equals(c.getEnderecoSelecionadoId()))
                        .findFirst()
                        .map(ResponseEntity::ok))
                .orElseGet(() -> clienteService.buscarClientePorEmail(user.getUsername())
                        .filter(c -> !c.getEnderecos().isEmpty())
                        .map(c -> ResponseEntity.ok(
                                c.getEnderecos().iterator().next()))
                        .orElse(ResponseEntity.noContent().build()));
    }

    /**
     * Adiciona um endereço para o cliente logado e o seleciona automaticamente.
     * Usado pelo formulário inline do checkout.
     */
    @PostMapping("/enderecos")
    public ResponseEntity<Void> adicionarEndereco(
            @RequestBody Endereco endereco,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        // Captura IDs existentes antes de adicionar, para identificar o novo
        Set<Long> idsAntes = clienteService.buscarClientePorEmail(user.getUsername())
                .map(c -> c.getEnderecos().stream()
                        .map(Endereco::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());

        clienteService.adicionarEnderecoParaUsuarioLogado(user.getUsername(), endereco);

        // Seleciona o novo endereço automaticamente
        clienteService.buscarClientePorEmail(user.getUsername())
                .flatMap(c -> c.getEnderecos().stream()
                        .filter(e -> e.getId() != null && !idsAntes.contains(e.getId()))
                        .findFirst()
                        .map(Endereco::getId))
                .ifPresent(id -> clienteService.selecionarEnderecoParaUsuarioLogado(user.getUsername(), id));

        return ResponseEntity.ok().build();
    }

    @GetMapping("/removerEndereco/{id}")
    public String removerEndereco(@PathVariable("id") Long enderecoId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String emailDoClienteLogado = principal.getName();
            Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                    .map(Cliente::getId)
                    .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

            clienteService.deletarEnderecoDoCliente(clienteId, enderecoId);

            redirectAttributes.addFlashAttribute("sucesso", "Endereço removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover endereço: " + e.getMessage());
        }

        return "redirect:/clientes/meu-perfil";
    }

    @GetMapping("/removerCartao/{id}")
    public String removerCartao(@PathVariable("id") Long cartaoId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            // Pega o email do usuário logado através do Principal
            String emailDoClienteLogado = principal.getName();

            // Busca o ID do cliente ou lança exceção se não encontrar
            Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                    .map(Cliente::getId)
                    .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

            // Chama o serviço para deletar o cartão específico do cliente
            clienteService.deletarCartaoDoCliente(clienteId, cartaoId);

            redirectAttributes.addFlashAttribute("sucesso", "Cartão removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover cartão: " + e.getMessage());
        }

        return "redirect:/clientes/homepage";
    }

    // ── MINHAS VENDAS ───────────────────────────────────────────────

    /**
     * Lista todos os livros anunciados pelo cliente autenticado.
     * Segurança: o email é extraído do JWT — o cliente só vê seus próprios
     * anúncios.
     */
    @GetMapping("/minhas-vendas")
    public ResponseEntity<List<VendaResponse.resumo>> listarMinhasVendas(
            @AuthenticationPrincipal UserDetails user) {
        if (user == null)
            return ResponseEntity.status(401).build();
        List<VendaResponse.resumo> vendas = minhasVendasService.listarMinhasVendas(user.getUsername());
        return ResponseEntity.ok(vendas);
    }

    /**
     * Detalhe de um anúncio específico.
     * Segurança: valida que o livro pertence ao cliente autenticado antes de
     * retornar.
     */
    @GetMapping("/minhas-vendas/{id}")
    public ResponseEntity<Object> detalharMinhaVenda(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        if (user == null)
            return ResponseEntity.status(401).build();
        try {
            VendaResponse dto = minhasVendasService.detalharMinhaVenda(id, user.getUsername());
            return ResponseEntity.ok(dto);
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
