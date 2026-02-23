package umc.exs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.design.factory.PagamentoFactory;
import umc.exs.design.strategy.PagamentoStrategy;
import umc.exs.model.dtos.user.ClienteDTO;
import umc.exs.model.dtos.user.CompraTokensRequestDTO;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.service.ClienteService;

@Slf4j
@RestController
@RequestMapping("/clientes/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final ClienteService clienteService;
    private final PagamentoFactory pagamentoFactory;

    @PostMapping("/comprar")
    public ResponseEntity<?> comprarTokens(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CompraTokensRequestDTO request) {

        PagamentoStrategy estrategia = pagamentoFactory.buscarEstrategia(request.getMetodoPagamento());
        boolean sucesso = estrategia.processar(request.getValor(), request);

        if (!sucesso)
            return ResponseEntity.badRequest().body("Pagamento recusado.");

        String email = userDetails.getUsername();
        Cliente cliente = clienteService.buscarEntidadePorEmail(email).get();

        // Chamada atualizada com registro de transação
        ClienteDTO atualizado = clienteService.adicionarTokens(
                cliente.getId(),
                request.getValor(),
                request.getMetodoPagamento(),
                request.getNumeroCartao());

        return ResponseEntity.ok(atualizado);
    }
}