package umc.exs.service.core.bussiness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.dto.response.admin.VendaResponse;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.enums.StatusVenda;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.PedidoRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinhasVendasService {

    private final LivroRepository livroRepository;
    private final PedidoRepository pedidoRepository;
    private final ObjectMapper objectMapper;

    /** Lista todos os livros anunciados pelo cliente autenticado. */
    public List<VendaResponse.resumo> listarMinhasVendas(String email) {
        List<Livro> livros = livroRepository.findAllByVendedorEmail(email);
        log.info("[MinhasVendas] email={} → {} livro(s) encontrado(s)", email, livros.size());
        return livros.stream()
                .map(this::toMinhaVendaDTO)
                .toList();
    }

    /**
     * Detalhe de um livro anunciado.
     * Valida que o livro pertence ao cliente autenticado — nunca expõe
     * dados de outro usuário (LGPD / segurança).
     */
    public VendaResponse detalharMinhaVenda(Long livroId, String email) {
        Livro livro = livroRepository.findByIdAndVendedorEmail(livroId, email)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Anúncio não encontrado ou não pertence ao seu perfil."));

        StatusVenda status = calcularStatus(livro);
        Optional<Pedido> pedidoOpt = status == StatusVenda.VENDIDO
                ? pedidoRepository.findByLivroId(livroId)
                : Optional.empty();

        VendaResponse dto = new VendaResponse();
        dto.setId(livro.getId());
        dto.setTitulo(livro.getTitulo());
        dto.setAutor(livro.getAutor());
        dto.setIsbn(livro.getIsbn());
        dto.setIdioma(livro.getIdioma());
        dto.setResumoOficial(livro.getResumoOficial());
        dto.setEstadoAprovado(livro.getEstadoAprovado());
        dto.setPrecoAprovado(livro.getPrecoAprovado());
        dto.setFotos(parseFotos(livro.getFotosUrls()));
        dto.setDataAnuncio(livro.getDataAnuncio());
        dto.setDataAprovacao(livro.getDataAprovacao());
        dto.setStatusVenda(status);
        dto.setMotivoRejeicao(livro.getMotivoRejeicao());
        pedidoOpt.ifPresent(p -> {
            dto.setDataVenda(p.getDataCompra());
            dto.setTokensRecebidos(p.getPrecoLivro());
        });
        return dto;
    }

    // ── helpers ──────────────────────────────────────────────────

    private VendaResponse.resumo toMinhaVendaDTO(Livro livro) {
        List<String> fotos = parseFotos(livro.getFotosUrls());
        String primeiraFoto = fotos.isEmpty() ? null : fotos.get(0);
        return new VendaResponse.resumo(
                livro.getId(),
                livro.getTitulo(),
                livro.getAutor(),
                livro.getIsbn(),
                livro.getEstadoAprovado(),
                livro.getPrecoAprovado(),
                primeiraFoto,
                livro.getDataAnuncio(),
                calcularStatus(livro),
                livro.getMotivoRejeicao());
    }

    private StatusVenda calcularStatus(Livro livro) {
        if (Boolean.TRUE.equals(livro.getAprovado())) {
            return pedidoRepository.existsByLivroId(livro.getId())
                    ? StatusVenda.VENDIDO
                    : StatusVenda.NA_VITRINE;
        }
        return livro.getAdminAprovadorId() != null
                ? StatusVenda.REJEITADO
                : StatusVenda.AGUARDANDO_APROVACAO;
    }

    @SuppressWarnings("unchecked")
    private List<String> parseFotos(String fotosUrls) {
        if (fotosUrls == null || fotosUrls.isBlank() || fotosUrls.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(fotosUrls, List.class);
        } catch (Exception e) {
            // Fallback: tenta separar por vírgula caso seja string simples
            return Arrays.stream(fotosUrls.replaceAll("[\\[\\]\"]", "").split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .toList();
        }
    }
}
