package umc.exs.service.core.bussiness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dtos.admin.AdminAprovacaoDTO;
import umc.exs.dtos.livro.LivroDTO;
import umc.exs.mappers.LivroMapper;
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

    private final LivroMapper livroMapper;

    private static final double TOKEN_REWARD = 10.0;
    private String message = "Livro não encontrado";

    // ========================= LISTAGENS =========================

    public List<LivroDTO> listarLivrosPendentes() {
        List<Livro> livros = livroRepository.findByAprovadoFalse();
        return converterLista(livros);
    }

    public List<LivroDTO> listarLivrosAprovados() {
        List<Livro> livros = livroRepository.findByAprovadoTrue();
        return converterLista(livros);
    }

    public List<LivroDTO> listarLivrosPorLote(Long loteId) {
        List<Livro> livros = livroRepository.findByLoteId(loteId);
        return converterLista(livros);
    }

    // ========================= APROVAÇÃO =========================

    @SuppressWarnings("null")
    @Transactional
    public LivroDTO aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(message));

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

        // NOTIFICAÇÕES
        try {
            listaDesejosService.notificarClientesSeDisponivel(anuncio.getIsbn(), anuncio.getTitulo());
        } catch (Exception e) {
            log.error("Erro wishlist: {}", e.getMessage());
        }

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            double saldoAntes = vendedor.getSaldoTokens() != null ? vendedor.getSaldoTokens() : 0.0;

            vendedor.setSaldoTokens(saldoAntes + TOKEN_REWARD);
            clienteRepository.save(vendedor);

            gamificacaoService.xpLivroAprovado(vendedor.getId());

            logAuditoria.registrarLog("LIVRO_APROVADO", vendedor.getId(), vendedor.getEmail(),
                    "Livro " + livroId);

            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Livro aprovado",
                        EmailHtmlBuilder.livroAprovado(vendedor.getNome(), anuncio.getTitulo(), TOKEN_REWARD));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
            }

            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Saldo atualizado",
                        EmailHtmlBuilder.atualizacaoSaldo(
                                vendedor.getNome(),
                                saldoAntes,
                                TOKEN_REWARD,
                                vendedor.getSaldoTokens(),
                                "Aprovação livro",
                                true,
                                LocalDateTime.now()));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro aprovado: {}", e.getMessage(), e);
            }
        }

        if (anuncio.getLote() != null) {
            Long loteId = anuncio.getLote().getId();

            long pending = livroRepository.countByLoteIdAndAprovadoFalse(loteId);

            if (pending == 0) {
                Lote lote = loteRepository.findById(loteId).orElseThrow();
                lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
                loteRepository.save(lote);
            }
        }

        return livroMapper.paraDTO(saved);
    }

    // ========================= REJEIÇÃO =========================

    @SuppressWarnings("null")
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String estado, String comentario) {

        Livro anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException(message));

        Cliente vendedor = anuncio.getVendedor();
        if (vendedor == null && anuncio.getLote() != null) {
            vendedor = anuncio.getLote().getCliente();
        }

        if (vendedor != null) {
            try {
                emailService.enviarHtml(
                        vendedor.getEmail(),
                        "Livro rejeitado",
                        EmailHtmlBuilder.livroRejeitado(
                                vendedor.getNome(),
                                anuncio.getTitulo(),
                                comentario));
            } catch (Exception e) {
                log.error("Erro ao enviar email de livro rejeitado: {}", e.getMessage(), e);
            }
        }

        Lote lote = anuncio.getLote();

        livroRepository.delete(anuncio);

        if (lote != null) {
            long pending = livroRepository.countByLoteIdAndAprovadoFalse(lote.getId());

            if (pending == 0) {
                long aprovados = livroRepository.findByLoteId(lote.getId()).size();

                lote.setStatus(aprovados == 0
                        ? Lote.LoteStatus.REJEITADO
                        : Lote.LoteStatus.PARCIAL_APROVADO);

                loteRepository.save(lote);
            }
        }

        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, "admin",
                "Livro " + livroId);
    }

    // ========================= CRUD ADMIN =========================

    @SuppressWarnings("null")
    @Transactional
    public LivroDTO adicionarLivroAdmin(String titulo, String autor, String isbn,
            Double preco, EstadoLivro estado, String capa, @NonNull Long vendedorId) {

        Cliente vendedor = clienteRepository.findById(vendedorId)
                .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));

        Livro livro = Livro.builder()
                .titulo(titulo)
                .autor(autor)
                .isbn(isbn)
                .precoAprovado(preco)
                .estadoAprovado(estado)
                .fotosUrls(capa)
                .vendedor(vendedor)
                .aprovado(false)
                .build();

        return livroMapper.paraDTO(livroRepository.save(livro));
    }

    @Transactional
    public LivroDTO editarLivroAdmin(@NonNull Long id, String titulo, String autor,
            String isbn, Double preco, EstadoLivro estado, String capa) {

        Livro livro = livroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(message));

        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setIsbn(isbn);
        livro.setPrecoAprovado(preco);
        livro.setEstadoAprovado(estado);
        livro.setFotosUrls(capa);

        return livroMapper.paraDTO(livroRepository.save(livro));
    }

    @Transactional
    public void deletarLivroAdmin(@NonNull Long id) {
        livroRepository.deleteById(id);
    }

    // ========================= CONVERSÃO =========================

    private List<LivroDTO> converterLista(List<Livro> livros) {
        List<LivroDTO> lista = new ArrayList<>();

        for (Livro livro : livros) {
            lista.add(livroMapper.paraDTO(livro));
        }

        return lista;
    }
}