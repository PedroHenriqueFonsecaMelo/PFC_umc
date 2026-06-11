package umc.exs.controller.api.control;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.exs.model.entidades.logic.Reporte;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.logic.ReporteRepository;
import umc.exs.repository.usuario.ClienteRepository;

/**
 * Recebe reportes e denúncias enviados pelos usuários via formulário e persiste no banco para análise do admin.
 * O endpoint é público, permitindo reportes mesmo de usuários não autenticados.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ReporteController {

    private final ClienteRepository clienteRepository;
    private final ReporteRepository reporteRepository;

    /**
     * Salva o reporte com motivo, e-mail e detalhes informados pelo usuário.
     * Enriquece o registro com nome, status e data de cadastro do cliente caso o e-mail esteja cadastrado.
     */
    @PostMapping("/api/reportes")
    public ResponseEntity<?> receberReporte(@RequestBody Map<String, String> body) {
        String motivo   = body.getOrDefault("motivo", "");
        String email    = body.getOrDefault("email", "");
        String detalhes = body.getOrDefault("detalhes", "");

        log.info("Reporte recebido: motivo='{}', email='{}'", motivo, email);

        Reporte.ReporteBuilder builder = Reporte.builder()
                .emailContato(email)
                .motivo(motivo)
                .detalhes(detalhes.isBlank() ? null : detalhes)
                .dataCriacao(LocalDateTime.now())
                .lido(false);

        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (clienteOpt.isPresent()) {
            Cliente c = clienteOpt.get();
            builder.nomeUsuario(c.getNome())
                   .statusConta(c.getStatusConta() != null ? c.getStatusConta().name() : null)
                   .dataCadastro(c.getDataCriacao());
        }

        reporteRepository.save(builder.build());

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
