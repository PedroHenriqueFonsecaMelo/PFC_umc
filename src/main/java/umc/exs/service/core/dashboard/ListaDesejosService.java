package umc.exs.service.core.dashboard;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.entidades.foundation.ListaDesejos;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.ListaDesejosRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;
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
    private final EmailFacade emailFacade;
    private final NotificacaoService notificacaoService;
    private final AppLogger appLogger;

    @Transactional
    public ListaDesejos adicionarDesejo(String emailCliente, String isbn) {

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

        ListaDesejos saved = listaDesejosRepository.save(desejo);

        appLogger.success(
                AcaoAuditoria.CLIENTE_DADOS_ATUALIZADOS,
                cliente.getId(),
                cliente.getEmail(),
                "ISBN adicionado à lista de desejos: " + isbn);

        log.info(
                "LISTA_DESEJOS_ADICIONAR clienteId={} email={} isbn={}",
                cliente.getId(),
                cliente.getEmail(),
                isbn);

        return saved;
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

        appLogger.success(
                AcaoAuditoria.CLIENTE_DADOS_ATUALIZADOS,
                cliente.getId(),
                cliente.getEmail(),
                "ISBN removido da lista de desejos: " + desejo.getIsbn());

        log.info(
                "LISTA_DESEJOS_REMOVER clienteId={} email={} isbn={}",
                cliente.getId(),
                cliente.getEmail(),
                desejo.getIsbn());
    }

    @Transactional(readOnly = true)
    public List<ListaDesejos> listarDesejos(String emailCliente) {

        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        List<ListaDesejos> desejos =
                listaDesejosRepository.findByClienteId(cliente.getId());

        log.debug(
                "LISTA_DESEJOS_LISTAR clienteId={} total={}",
                cliente.getId(),
                desejos.size());

        return desejos;
    }

    @Transactional
    public void notificarClientesSeDisponivel(String isbn, String titulo) {

        if (isbn == null || isbn.isBlank()) return;

        List<ListaDesejos> interessados =
                listaDesejosRepository.findByIsbn(isbn);

        if (interessados.isEmpty()) return;

        log.info(
                "LISTA_DESEJOS_NOTIFICACAO_DISPONIBILIDADE isbn={} total={}",
                isbn,
                interessados.size());

        for (ListaDesejos desejo : interessados) {

            try {
                Cliente cliente = desejo.getCliente();

                emailFacade.sendHtmlSafe(
                        cliente.getEmail(),
                        "Livro da sua lista de desejos disponível!",
                        EmailHtmlBuilder.listaDesejosDisponivel(
                                cliente.getNome(),
                                titulo,
                                isbn,
                                desejo.isPreReservaAtiva(),
                                baseUrl));

                notificacaoService.criarNotificacaoDashboard(
                        cliente,
                        "O livro '" + titulo + "' está disponível na vitrine!",
                        "/livros/vitrine");

                appLogger.info(
                        AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO,
                        cliente.getId(),
                        cliente.getEmail(),
                        "Notificação de disponibilidade enviada ISBN: " + isbn);

                log.info(
                        "LISTA_DESEJOS_NOTIFICADO clienteId={} email={} isbn={}",
                        cliente.getId(),
                        cliente.getEmail(),
                        isbn);

            } catch (Exception e) {

                log.error(
                        "LISTA_DESEJOS_NOTIFICACAO_ERRO email={} isbn={}",
                        desejo.getCliente().getEmail(),
                        isbn,
                        e);
            }
        }
    }

    @Transactional
    public void notificarClientesSeEmPromocao(String isbn, String titulo, double precoPromo) {

        if (isbn == null || isbn.isBlank()) return;

        List<ListaDesejos> interessados =
                listaDesejosRepository.findByIsbn(isbn);

        if (interessados.isEmpty()) return;

        log.info(
                "LISTA_DESEJOS_NOTIFICACAO_PROMOCAO isbn={} total={}",
                isbn,
                interessados.size());

        for (ListaDesejos desejo : interessados) {

            try {

                notificacaoService.criarNotificacaoDashboard(
                        desejo.getCliente(),
                        "O livro '" + titulo + "' entrou em promoção por T$ " + precoPromo,
                        "/livros/vitrine");

                appLogger.info(
                        AcaoAuditoria.CLIENTE_PERFIL_VISUALIZADO,
                        desejo.getCliente().getId(),
                        desejo.getCliente().getEmail(),
                        "Notificação de promoção ISBN: " + isbn);

            } catch (Exception e) {

                log.error(
                        "LISTA_DESEJOS_PROMOCAO_ERRO email={} isbn={}",
                        desejo.getCliente().getEmail(),
                        isbn,
                        e);
            }
        }
    }

    @Transactional
    public ListaDesejos togglePreReserva(String emailCliente, @NonNull Long desejoId) {

        Cliente cliente = clienteRepository.findByEmail(emailCliente)
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        ListaDesejos desejo = listaDesejosRepository.findById(desejoId)
                .orElseThrow(() -> new RuntimeException("Item não encontrado na lista de desejos"));

        if (!desejo.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Acesso negado: este item não pertence ao seu perfil");
        }

        desejo.setPreReservaAtiva(!desejo.isPreReservaAtiva());

        ListaDesejos updated = listaDesejosRepository.save(desejo);

        appLogger.success(
                AcaoAuditoria.CLIENTE_DADOS_ATUALIZADOS,
                cliente.getId(),
                cliente.getEmail(),
                "Toggle pré-reserva ISBN: " + desejo.getIsbn());

        log.info(
                "LISTA_DESEJOS_TOGGLE_PRE_RESERVA clienteId={} email={} isbn={} status={}",
                cliente.getId(),
                cliente.getEmail(),
                desejo.getIsbn(),
                desejo.isPreReservaAtiva());

        return updated;
    }
}