package umc.exs.service.core.livros.avaliacao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import umc.exs.dto.request.cliente.ComentarioRequest;
import umc.exs.dto.request.livro.AvaliacaoLivroRequest;
import umc.exs.model.entidades.livro.AvaliacaoLivro;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.livro.Obra;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.livro.AvaliacaoLivroRepository;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.livro.ObraRpository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.LogAuditoriaService;

@Service
@RequiredArgsConstructor
@Transactional
public class LivroAvaliacaoService {

        private static final String LOG_AVALIACAO_CRIADA = "LIVRO_AVALIACAO_CRIADA";
        private static final String LOG_AVALIACAO_CONSULTA = "LIVRO_AVALIACAO_CONSULTA";
        private static final String LOG_MEDIA_CALCULADA = "LIVRO_AVALIACAO_MEDIA_CALCULADA";
        private static final String LOG_LIVRO_REFERENCIA_CRIADO = "LIVRO_REFERENCIA_CRIADO";

        private final AvaliacaoLivroRepository avaliacaoRepository;
        private final ClienteRepository clienteRepository;
        private final LivroRepository livroRepository;
        private final ObraRpository obraRepo;
        private final LogAuditoriaService logAuditoria;
        private final GamificacaoService gamificacaoService;

        public AvaliacaoLivro criarAvaliacao(String email, AvaliacaoLivroRequest dto) {

                Cliente avaliador = clienteRepository.findByEmail(email)
                                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

                Livro livroReferencia = livroRepository.findByIsbn(dto.getIsbn())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Livro com ISBN " + dto.getIsbn() + " não catalogado."));

                Obra obra = livroReferencia.getObra();

                validarNota(dto.getNota());

                if (obra == null) {
                        throw new IllegalStateException("Livro sem obra vinculada.");
                }

                boolean jaAvaliouObra = avaliacaoRepository
                                .existsByObraIdAndAvaliadorId(obra.getId(), avaliador.getId());

                if (jaAvaliouObra) {
                        throw new IllegalStateException("Você já avaliou esta obra.");
                }

                AvaliacaoLivro avaliacao = AvaliacaoLivro.builder()
                                .obra(obra)
                                .tituloLivro(dto.getTituloLivro())
                                .comentario(dto.getComentario())
                                .nota(dto.getNota())
                                .dataAvaliacao(LocalDateTime.now())
                                .avaliador(avaliador)
                                .isbnOriginalNoAto(dto.getIsbn())
                                .build();

                AvaliacaoLivro saved = avaliacaoRepository.save(avaliacao);

                gamificacaoService.xpAvaliacao(avaliador.getId());

                logAuditoria.registrarLog(
                                LOG_AVALIACAO_CRIADA,
                                avaliador.getId(),
                                avaliador.getEmail(),
                                "Obra=" + obra.getTitulo() + ", Livro=" + dto.getTituloLivro() + ", Nota="
                                                + dto.getNota());

                return saved;
        }

        public List<AvaliacaoLivro> buscarAvaliacoesUnificadas(String isbn) {

                Livro livro = livroRepository.findByIsbn(isbn)
                                .orElseThrow(() -> new EntityNotFoundException("ISBN não encontrado"));

                Obra obra = livro.getObra();

                if (obra == null) {
                        throw new IllegalStateException("Livro sem obra vinculada.");
                }

                List<AvaliacaoLivro> avaliacoes = avaliacaoRepository
                                .findByObraIdOrderByDataAvaliacaoDesc(obra.getId());

                logAuditoria.registrarLog(
                                LOG_AVALIACAO_CONSULTA,
                                "ObraId=" + obra.getId() + ", Total=" + avaliacoes.size());

                return avaliacoes;
        }

        public Double calcularMediaUnificada(String isbn) {

                Livro livro = livroRepository.findByIsbn(isbn)
                                .orElseThrow(() -> new EntityNotFoundException("ISBN não encontrado"));

                Obra obra = livro.getObra();

                if (obra == null) {
                        return null;
                }

                Double media = avaliacaoRepository.getAverageRatingByObraId(obra.getId());

                logAuditoria.registrarLog(
                                LOG_MEDIA_CALCULADA,
                                "ObraId=" + obra.getId() + ", Media=" + media);

                return media;
        }

        public Double calcularMediaPorIsbn(String isbn) {
                return livroRepository.findByIsbn(isbn)
                                .map(livro -> {
                                        Obra obra = livro.getObra();
                                        Double media = obra != null
                                                        ? avaliacaoRepository.getAverageRatingByObraId(obra.getId())
                                                        : null;

                                        logAuditoria.registrarLog(
                                                        LOG_MEDIA_CALCULADA,
                                                        "ISBN=" + isbn + ", Media=" + media);

                                        return media;
                                })
                                .orElse(null);
        }

        private void validarNota(Integer nota) {
                if (nota == null || nota < 1 || nota > 5) {
                        throw new IllegalArgumentException("A nota deve ser entre 1 e 5");
                }
        }

        public Livro criarLivroReferencia(ComentarioRequest dto) {

                Livro livro = livroRepository.findByIsbn(dto.getIsbn())
                                .orElseGet(() -> {

                                        Obra novaObra = obraRepo.save(
                                                        Obra.builder()
                                                                        .titulo(dto.getTitulo())
                                                                        .autor(dto.getAutor())
                                                                        .build());

                                        return livroRepository.save(
                                                        Livro.builder()
                                                                        .isbn(dto.getIsbn())
                                                                        .titulo(dto.getTitulo())
                                                                        .obra(novaObra)
                                                                        .build());
                                });

                logAuditoria.registrarLog(
                                LOG_LIVRO_REFERENCIA_CRIADO,
                                "ISBN=" + dto.getIsbn() + ", Titulo=" + dto.getTitulo());

                return livro;
        }
}