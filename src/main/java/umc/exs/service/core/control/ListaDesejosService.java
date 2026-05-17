package umc.exs.service.core.control;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.dto.compra.lote.ListaDesejosDTO;
import umc.exs.model.entidades.foundation.ListaDesejos;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.ListaDesejosRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.notificacao.NotificacaoService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListaDesejosService {

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    private final ListaDesejosRepository listaDesejosRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;
    private final NotificacaoService notificacaoService;

    @Transactional
    public ListaDesejosDTO adicionarDesejo(String emailCliente, String isbn) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        if (listaDesejosRepository.existsByClienteIdAndIsbn(cliente.getId(), isbn)) {
            throw new RuntimeException("ISBN já está na sua lista de desejos");
        }

        ListaDesejos desejo = ListaDesejos.builder()
                .cliente(cliente)
                .isbn(isbn)
                .dataAdicao(LocalDateTime.now())
                .build();

        if (desejo != null) {
            ListaDesejos salvo = listaDesejosRepository.save(desejo);
            return toDTO(salvo);
        } else {
            return null;
        }

    }

    @Transactional
    public void removerDesejo(String emailCliente, @NonNull Long desejoId) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        ListaDesejos desejo = listaDesejosRepository.findById(desejoId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado na lista de desejos"));

        if (!desejo.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Acesso negado: este item não pertence ao seu perfil");
        }

        listaDesejosRepository.delete(desejo);
    }

    @Transactional(readOnly = true)
    public List<ListaDesejosDTO> listarDesejos(String emailCliente) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        return listaDesejosRepository.findByClienteId(cliente.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Notifica via e-mail e dashboard todos os clientes que têm o ISBN na lista de desejos
     * quando um livro com esse ISBN é aprovado e fica disponível na vitrine.
     */
    @SuppressWarnings("null")
    @Transactional
    public void notificarClientesSeDisponivel(String isbn, String titulo) {
        if (isbn == null || isbn.isBlank()) {
            return;
        }

        List<ListaDesejos> interessados = listaDesejosRepository.findByIsbn(isbn);
        if (interessados.isEmpty()) {
            return;
        }

        log.info("Notificando {} cliente(s) sobre disponibilidade do livro ISBN {}", interessados.size(), isbn);

        for (ListaDesejos desejo : interessados) {
            try {
                Cliente cliente = desejo.getCliente();
                String assunto = desejo.isPreReservaAtiva()
                        ? "Pré-reserva ativada — Livro disponível! — Bibliotroca"
                        : "Livro da sua lista de desejos disponível! — Bibliotroca";

                emailService.enviarHtml(
                        cliente.getEmail(), assunto,
                        EmailHtmlBuilder.listaDesejosDisponivel(
                                cliente.getNome(), titulo, isbn, desejo.isPreReservaAtiva(), baseUrl));

                // Notificação dashboard
                notificacaoService.criarNotificacaoDashboard(
                        cliente,
                        String.format("O livro '%s' da sua lista de desejos está disponível na vitrine!", titulo),
                        "/livros/vitrine");

                log.info("Notificação enviada para {} sobre ISBN {} (pré-reserva: {})",
                        cliente.getEmail(), isbn, desejo.isPreReservaAtiva());
            } catch (Exception e) {
                log.error("Falha ao notificar cliente {} sobre ISBN {}: {}", desejo.getCliente().getEmail(), isbn,
                        e.getMessage());
            }
        }
    }

    /**
     * Notifica via dashboard todos os clientes que têm o ISBN na lista de desejos
     * quando o livro entra em promoção.
     */
    @Transactional
    public void notificarClientesSeEmPromocao(String isbn, String titulo, double precoPromo) {
        if (isbn == null || isbn.isBlank()) return;

        List<ListaDesejos> interessados = listaDesejosRepository.findByIsbn(isbn);
        if (interessados.isEmpty()) return;

        log.info("Notificando {} cliente(s) sobre promoção do livro ISBN {}", interessados.size(), isbn);

        for (ListaDesejos desejo : interessados) {
            try {
                notificacaoService.criarNotificacaoDashboard(
                        desejo.getCliente(),
                        String.format("O livro '%s' que você deseja está em promoção por T$ %.2f!", titulo, precoPromo),
                        "/livros/vitrine");
            } catch (Exception e) {
                log.error("Falha ao notificar cliente {} sobre promoção ISBN {}: {}",
                        desejo.getCliente().getEmail(), isbn, e.getMessage());
            }
        }
    }

    @Transactional
    public ListaDesejosDTO togglePreReserva(String emailCliente, @NonNull Long desejoId) {
        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        ListaDesejos desejo = listaDesejosRepository.findById(desejoId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado na lista de desejos"));

        if (!desejo.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Acesso negado: este item não pertence ao seu perfil");
        }

        desejo.setPreReservaAtiva(!desejo.isPreReservaAtiva());
        return toDTO(listaDesejosRepository.save(desejo));
    }

    private ListaDesejosDTO toDTO(ListaDesejos l) {
        return ListaDesejosDTO.builder()
                .id(l.getId())
                .isbn(l.getIsbn())
                .dataAdicao(l.getDataAdicao())
                .preReservaAtiva(l.isPreReservaAtiva())
                .build();
    }
}
