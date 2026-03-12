package umc.exs.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.model.dtos.AvaliacaoLivroDTO;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.daos.repository.AvaliacaoLivroRepository;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.entidades.foundation.AvaliacaoLivro;
import umc.exs.model.entidades.usuario.Cliente;

@Service
@RequiredArgsConstructor
public class AvaliacaoLivroService {

    private final AvaliacaoLivroRepository avaliacaoRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    /**
     * Create a new book review
     */
    @Transactional
    public AvaliacaoLivro criarAvaliacao(String email, AvaliacaoLivroDTO dto) {
        // Buscar cliente
        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (!clienteOpt.isPresent()) {
            throw new RuntimeException("Usuário não encontrado");
        }
        Cliente avaliador = clienteOpt.get();

        // Validar nota
        if (dto.getNota() == null || dto.getNota() < 1 || dto.getNota() > 5) {
            throw new RuntimeException("A nota deve ser entre 1 e 5");
        }

        // Validar campos obrigatórios
        if (dto.getIsbn() == null || dto.getIsbn().trim().isEmpty()) {
            throw new RuntimeException("ISBN é obrigatório");
        }
        if (dto.getTituloLivro() == null || dto.getTituloLivro().trim().isEmpty()) {
            throw new RuntimeException("Título do livro é obrigatório");
        }

        // Verificar se o usuário já avaliou este livro
        boolean jaAvaliou = avaliacaoRepository.existsByIsbnAndAvaliadorId(dto.getIsbn(), avaliador.getId());
        if (jaAvaliou) {
            throw new RuntimeException("Você já avaliou este livro");
        }

        // Criar avaliação
        AvaliacaoLivro avaliacao = new AvaliacaoLivro();
        avaliacao.setIsbn(dto.getIsbn());
        avaliacao.setTituloLivro(dto.getTituloLivro());
        avaliacao.setNota(dto.getNota());
        avaliacao.setComentario(dto.getComentario());
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        avaliacao.setAvaliador(avaliador);

        // Salvar
        AvaliacaoLivro saved = avaliacaoRepository.save(avaliacao);

        // Registrar auditoria
        logAuditoria.registrarLog("AVALIACAO_CRIADA", avaliador.getId(), avaliador.getEmail(), 
            "Avaliou o livro '" + dto.getTituloLivro() + "' com nota " + dto.getNota());

        return saved;
    }

    /**
     * Get all reviews for a specific book by ISBN
     */
    public List<AvaliacaoLivro> buscarAvaliacoesPorIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            throw new RuntimeException("ISBN é obrigatório");
        }
        
        List<AvaliacaoLivro> todasAvaliacoes = avaliacaoRepository.findAll();
        List<AvaliacaoLivro> filtradas = new ArrayList<>();
        
        for (AvaliacaoLivro avaliacao : todasAvaliacoes) {
            if (avaliacao.getIsbn() != null && avaliacao.getIsbn().equals(isbn)) {
                filtradas.add(avaliacao);
            }
        }
        
        return filtradas;
    }

    /**
     * Get average rating for a book
     */
    public Double calcularMediaPorIsbn(String isbn) {
        List<AvaliacaoLivro> avaliacoes = buscarAvaliacoesPorIsbn(isbn);
        
        if (avaliacoes.isEmpty()) {
            return null;
        }
        
        int soma = 0;
        int quantidade = 0;
        
        for (AvaliacaoLivro avaliacao : avaliacoes) {
            if (avaliacao.getNota() != null) {
                soma = soma + avaliacao.getNota();
                quantidade++;
            }
        }
        
        if (quantidade == 0) {
            return null;
        }
        
        return (double) soma / quantidade;
    }

    /**
     * Get all unique books that have reviews
     */
    public List<String> buscarLivrosComAvaliacoes() {
        List<AvaliacaoLivro> todasAvaliacoes = avaliacaoRepository.findAll();
        List<String> livros = new ArrayList<>();
        
        for (AvaliacaoLivro avaliacao : todasAvaliacoes) {
            String isbn = avaliacao.getIsbn();
            if (isbn != null && !livros.contains(isbn)) {
                livros.add(isbn);
            }
        }
        
        return livros;
    }
}

