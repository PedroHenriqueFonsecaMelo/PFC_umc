package umc.exs.controller.api.contas;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import umc.exs.dtos.user.EnderecoDTO;

import lombok.RequiredArgsConstructor;
import umc.exs.dtos.user.ClienteDTO;
import umc.exs.model.entidades.foundation.Transacao;
import umc.exs.service.core.cliente.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClientControllerApi {

    private static final String CLIENTE_NAO_ENCONTRADO = "Cliente não encontrado.";

    private final ClienteService clienteService;

    /** Retorna dados do perfil para atualizar saldo/nome via JS */
    @GetMapping("/meu-perfil")
    public ResponseEntity<ClienteDTO> getPerfilJson(@AuthenticationPrincipal UserDetails user) {
        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Retorna histórico de transações em JSON */
    @GetMapping("/transacoes")
    public ResponseEntity<List<Transacao>> getHistorico(@AuthenticationPrincipal UserDetails user) {
        Long id = clienteService.buscarClientePorEmail(user.getUsername()).map(ClienteDTO::getId).orElseThrow();
        return ResponseEntity.ok(clienteService.listarHistoricoTransacoes(id));
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
    public ResponseEntity<ClienteDTO> perfilJson(@AuthenticationPrincipal UserDetails user) {

        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return clienteService.buscarClientePorEmail(user.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).build());
    }

    /** Retorna o endereço selecionado para entrega do cliente logado */
    @GetMapping("/endereco-selecionado")
    public ResponseEntity<EnderecoDTO> getEnderecoSelecionado(@AuthenticationPrincipal UserDetails user) {
        return clienteService.buscarClientePorEmail(user.getUsername())
                .flatMap(c -> c.getEnderecos().stream()
                        .filter(e -> e.getId() != null && e.getId().equals(c.getEnderecoSelecionadoId()))
                        .findFirst()
                        .map(ResponseEntity::ok))
                .orElseGet(() ->
                        // Se nenhum selecionado mas tem endereço, retorna o primeiro
                        clienteService.buscarClientePorEmail(user.getUsername())
                                .filter(c -> !c.getEnderecos().isEmpty())
                                .map(c -> ResponseEntity.ok(c.getEnderecos().get(0)))
                                .orElse(ResponseEntity.noContent().build())
                );
    }

    @GetMapping("/removerEndereco/{id}")
    public String removerEndereco(@PathVariable("id") Long enderecoId, Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            String emailDoClienteLogado = principal.getName();
            Long clienteId = clienteService.buscarClientePorEmail(emailDoClienteLogado)
                    .map(ClienteDTO::getId)
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
                    .map(ClienteDTO::getId)
                    .orElseThrow(() -> new RuntimeException(CLIENTE_NAO_ENCONTRADO));

            // Chama o serviço para deletar o cartão específico do cliente
            clienteService.deletarCartaoDoCliente(clienteId, cartaoId);

            redirectAttributes.addFlashAttribute("sucesso", "Cartão removido com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", "Erro ao remover cartão: " + e.getMessage());
        }

        return "redirect:/clientes/homepage";
    }
}
