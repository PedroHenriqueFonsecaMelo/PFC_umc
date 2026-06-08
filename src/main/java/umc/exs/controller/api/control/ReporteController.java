package umc.exs.controller.api.control;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.exs.model.entidades.foundation.NotificacaoDashboard;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.NotificacaoDashboardRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReporteController {

    private final ClienteRepository clienteRepository;
    private final NotificacaoDashboardRepository notificacaoRepository;

    @PostMapping("/api/reportes")
    public ResponseEntity<?> receberReporte(@RequestBody Map<String, String> body) {
        String motivo = body.getOrDefault("motivo", "");
        String email = body.getOrDefault("email", "");
        String detalhes = body.getOrDefault("detalhes", "");

        log.info("Reporte recebido: motivo='{}', email='{}'", motivo, email);

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (clienteOpt.isPresent()) {
            String mensagem = "[REPORTE] " + motivo
                    + (detalhes.isBlank() ? "" : " | Detalhes: " + detalhes);

            NotificacaoDashboard notif = new NotificacaoDashboard();
            notif.setCliente(clienteOpt.get());
            notif.setMensagem(mensagem);
            notif.setDataCriacao(LocalDateTime.now());
            notif.setLida(false);
            notif.setLink("/admin/usuarios");
            notificacaoRepository.save(notif);
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
