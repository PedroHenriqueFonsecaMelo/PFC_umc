package umc.exs.service.core.interactions;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.livro.AvaliacaoLivroDTO;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

@Service
@RequiredArgsConstructor
public class AvaliacaoLivroService {

    private final AvaliacaoLivroRepository avaliacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LivroRepository livroRepository;
    private final LogAuditoriaService logAuditoria;
    private final GamificacaoService gamificacaoService;

    /**
     * Cria uma avaliação unificada por Obra
     */
    @SuppressWarnings("null")
    @Transactional
    public AvaliacaoLivro criarAvaliacao(String email, AvaliacaoLivroDTO dto) {
        // 1. Validações de Usuário
        Cliente avaliador = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // 2. Localiza o Livro e sua respectiva Obra
        Livro livroReferencia = livroRepository.findByIsbn(dto.getIsbn())
                .orElseThrow(() -> new RuntimeException("Livro com ISBN " + dto.getIsbn() + " não catalogado."));

        Obra obra = livroReferencia.getObra();
        if (obra == null) {
            throw new RuntimeException("Este livro não possui uma Obra vinculada para agrupamento.");
        }

        // 3. Validações de Negócio
        validarNota(dto.getNota());

        // 4. Verifica se o usuário já avaliou ESTA OBRA (independente do ISBN)
        boolean jaAvaliouObra = avaliacaoRepository.existsByObraIdAndAvaliadorId(obra.getId(), avaliador.getId());
        if (jaAvaliouObra) {
            throw new RuntimeException("Você já avaliou esta obra (em outra edição ou tradução).");
        }

        // 5. Instancia e Salva
        AvaliacaoLivro avaliacao = AvaliacaoLivro.builder()
                .obra(obra)
                .tituloLivro(dto.getTituloLivro())
                .comentario(dto.getComentario())
                .nota(dto.getNota())
                .dataAvaliacao(LocalDateTime.now())
                .avaliador(avaliador)
                .isbnOriginalNoAto(dto.getIsbn().toString())
                .build();

        AvaliacaoLivro saved = avaliacaoRepository.save(avaliacao);

        // 6. Pós-processamento
        gamificacaoService.xpAvaliacao(avaliador.getId());
        logAuditoria.registrarLog("AVALIACAO_CRIADA", avaliador.getId(), avaliador.getEmail(),
                "Avaliou a obra '" + obra.getTituloOriginal() + "' via edição '" + dto.getTituloLivro() + "'");

        return saved;
    }

    /**
     * Retorna todas as avaliações de uma obra baseada em qualquer ISBN dela
     */
    public List<AvaliacaoLivro> buscarAvaliacoesUnificadas(String isbn) {
        Livro livro = livroRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RuntimeException("ISBN não encontrado"));

        return avaliacaoRepository.findByObraIdOrderByDataAvaliacaoDesc(livro.getObra().getId());
    }

    /**
     * Calcula a média global da Obra (todas as traduções/edições juntas)
     */
    public Double calcularMediaUnificada(String isbn) {
        Livro livro = livroRepository.findByIsbn(isbn)
                .orElseThrow(() -> new RuntimeException("ISBN não encontrado"));

        return avaliacaoRepository.getAverageRatingByObraId(livro.getObra().getId());
    }

    public Double calcularMediaPorIsbn(String isbn) {
        return livroRepository.findByIsbn(isbn)
            .map(livro -> {
                if (livro.getObra() == null) return null;
                return avaliacaoRepository.getAverageRatingByObraId(livro.getObra().getId());
            })
            .orElse(null);
    }

    private void validarNota(Integer nota) {
        if (nota == null || nota < 1 || nota > 5) {
            throw new RuntimeException("A nota deve ser entre 1 e 5");
        }
    }
}