package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.DTOs.admin.AdminAprovacaoDTO;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.control.ListaDesejosService;
import umc.exs.service.email.EmailHtmlBuilder;
import umc.exs.service.email.EmailService;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivroAdminService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LoteRepository loteRepository;
    private final LogAuditoriaService logAuditoria;
    private final EmailService emailService;
    private final ListaDesejosService listaDesejosService;
    private final GamificacaoService gamificacaoService;

    private static final double TOKEN_REWARD = 10.0;

    public List<Livro> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    public List<Livro> listarLivrosPorLote(Long loteId) {
        return livroRepository.findByLoteId(loteId);
    }

    /**
     * Aprova livro admin define preço/estado.
     * Transfer system, update lote status se completo.
     * 
     * @param livroId ID
     * @param adminId aprovador
     * @param dto     aprovação
     */
    @SuppressWarnings("null")
    @Transactional
    public Livro aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());
        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco());
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());
        if (dto.getFotosUrls() != null && !dto.getFotosUrls().isBlank()) {
            anuncio.setFotosUrls(dto.getFotosUrls());
        }

        Livro saved = livroRepository.save(anuncio);

        // Notificar clientes interessados via lista de desejos
        try {
            listaDesejosService.notificarClientesSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());
        } catch (Exception e) {
            log.error("Falha ao notificar lista de desejos para ISBN {}: {}", anuncio.getIsbn(), e.getMessage());
        }

        // Identificar o vendedor: campo direto ou via lote
        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            // Creditar tokens ao vendedor apenas na aprovação
            vendedor.setSaldoTokens(vendedor.getSaldoTokens() + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            // Gamificação: XP ao vendedor
            gamificacaoService.xpLivroAprovado(vendedor.getId());

            logAuditoria.registrarLog("LIVRO_APROVADO_RECOMPENSA", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + livroId + " aprovado - T$" + TOKEN_REWARD + " creditados");

            // E-mail de confirmação ao vendedor
            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Seu livro foi aprovado! — Bibliotroca",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), TOKEN_REWARD));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de aprovação para vendedor {}: {}", vendedor.getEmail(), e.getMessage());
            }
        }

        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();
            long pendingCount = livroRepository.countPendingByLoteId(loteId);
            if (pendingCount == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return saved;
    }

    /**
     * Rejeita livro com comentário admin.
     * Set aprovado=false, comentarioAprovacao.
     * Log LIVRO_REJEITADO.
     */
    @SuppressWarnings("null")
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        if (!estado.equalsIgnoreCase(EstadoLivro.RUIM.name())) {
            throw new RuntimeException("Apenas livros com estado RUIM ou pior podem ser rejeitados");
        }

        // E-mail de rejeição ao vendedor (antes de deletar)
        Cliente vendedorRejeicao = anuncio.getVendedor();
        if (vendedorRejeicao == null && anuncio.getLote() != null) {
            vendedorRejeicao = anuncio.getLote().getCliente();
        }
        if (vendedorRejeicao != null) {
            final String emailVendedor = vendedorRejeicao.getEmail();
            final String nomeVendedor = vendedorRejeicao.getNome();
            final String tituloLivro = anuncio.getTitulo();
            try {
                emailService.enviarHtml(
                        emailVendedor,
                        "Livro não aprovado — Bibliotroca",
                        EmailHtmlBuilder.livroRejeitado(nomeVendedor, tituloLivro, comentario));
            } catch (Exception e) {
                log.error("Falha ao enviar e-mail de rejeição para vendedor {}: {}", emailVendedor, e.getMessage());
            }
        }

        livroRepository.delete(anuncio);

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin#" + adminId,
                "Livro ID " + livroId + " rejeitado pelo administrador.");
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroRepository.deleteById(id);
    }

    /**
     * Adiciona um livro diretamente via painel administrativo
     */
    @SuppressWarnings("null")
    @Transactional
    public Livro adicionarLivroAdmin(String titulo, String autor, String isbn, Double preco, EstadoLivro estado, String capa, @NonNull Long vendedorId) {
        Cliente vendedor = clienteRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));

        Livro novoLivro = Livro.builder()
                .titulo(titulo)
                .autor(autor)
                .isbn(isbn)
                .precoAprovado(preco)
                .estadoAprovado(estado)
                .fotosUrls(capa)
                .vendedor(vendedor)
                .aprovado(false)
                .build();

        return livroRepository.save(novoLivro);
    }

    /**
     * Edita um livro existente via painel administrativo
     */
    @Transactional
    public Livro editarLivroAdmin(@NonNull Long id, String titulo, String autor, String isbn, Double preco, EstadoLivro estado, String capa) {
        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setIsbn(isbn);
        livro.setPrecoAprovado(preco);
        livro.setEstadoAprovado(estado);
        livro.setFotosUrls(capa);

        return livroRepository.save(livro);
    }
}