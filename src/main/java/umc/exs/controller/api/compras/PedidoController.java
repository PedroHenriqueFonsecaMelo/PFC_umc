package umc.exs.controller.api.compras;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.mapper.PedidoMapper;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.cliente.ClienteService;
import umc.exs.service.core.dashboard.PedidoService;
import umc.exs.service.storage.EtiquetaService;

/**
 * Fornece endpoints REST para o cliente autenticado consultar seus pedidos e gerar etiquetas de envio.
 * Todos os endpoints exigem autenticação e operam apenas sobre os pedidos do próprio cliente.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final PedidoMapper pedidoMapper;
    private final EtiquetaService etiquetaService;

    /**
     * Retorna os pedidos do cliente autenticado que ainda estão aguardando envio ou em trânsito.
     * Responde 401 caso o usuário não esteja autenticado.
     */
    @GetMapping("/pendentes")
    public ResponseEntity<?> listarPendentes(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        // Converte o e-mail do token JWT no ID interno do cliente
        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarPendentes(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista));
    }

    /**
     * Retorna os pedidos do cliente autenticado que já foram entregues ou concluídos.
     * Responde 401 caso o usuário não esteja autenticado.
     */
    @GetMapping("/concluidos")
    public ResponseEntity<?> listarConcluidos(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        // Converte o e-mail do token JWT no ID interno do cliente
        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarConcluidos(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista));
    }

    /**
     * Retorna todos os pedidos do cliente autenticado, independentemente do status de envio.
     * Responde 401 caso o usuário não esteja autenticado.
     */
    @GetMapping("/todos")
    public ResponseEntity<?> listarTodos(@AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        // Converte o e-mail do token JWT no ID interno do cliente
        Long id = resolverId(user.getUsername());

        var lista = pedidoService.listarPorCliente(id);

        return ResponseEntity.ok(
                pedidoMapper.toResponseList(lista));
    }

    /**
     * Busca um pedido específico pelo ID, garantindo que ele pertença ao cliente autenticado.
     * Retorna 404 caso o pedido não exista ou pertença a outro cliente.
     */
    @GetMapping("/{pedidoId}")
    public ResponseEntity<?> buscarPorId(
            @PathVariable Long pedidoId,
            @AuthenticationPrincipal UserDetails user) {

        if (user == null)
            return ResponseEntity.status(401).build();

        // Resolve o ID do cliente para garantir que só veja seus próprios pedidos
        Long clienteId = resolverId(user.getUsername());

        return pedidoService.buscarPorIdEComprador(pedidoId, clienteId)
                .map(pedidoMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Gera e retorna a etiqueta de envio do pedido em formato PDF para download.
     * Retorna 404 se o pedido não existir e 500 em caso de falha na geração do PDF.
     */
    @GetMapping("/{id}/etiqueta")
    public ResponseEntity<byte[]> gerarEtiqueta(@PathVariable Long id) {
        try {
            byte[] pdf = etiquetaService.gerarEtiqueta(id);

            // Define os cabeçalhos para que o navegador trate a resposta como download de PDF
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment", "etiqueta-pedido-" + id + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Converte o e-mail do usuário autenticado no ID interno do cliente no banco de dados.
     * Lança exceção caso o e-mail não corresponda a nenhum cliente cadastrado.
     */
    private Long resolverId(String email) {

        return clienteService.buscarClientePorEmail(email)
                .map(Cliente::getId)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));
    }
}
