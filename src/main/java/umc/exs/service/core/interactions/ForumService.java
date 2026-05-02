package umc.exs.service.core.interactions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.forum.NovaRespostaDTO;
import umc.exs.DTOs.forum.NovoTopicoDTO;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final TopicoForumRepository topicoRepo;
    private final RespostaForumRepository respostaRepo;
    private final ClienteRepository clienteRepo;

    // ── Listagem ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TopicoForum> listarTopicos(String busca, CategoriaForum categoria, Pageable pageable) {
        boolean temBusca = busca != null && !busca.isBlank();
        boolean temCategoria = categoria != null;

        if (temBusca && temCategoria) {
            return topicoRepo.findByTituloContainingIgnoreCaseAndCategoria(busca, categoria, pageable);
        } else if (temBusca) {
            return topicoRepo.findByTituloContainingIgnoreCase(busca, pageable);
        } else if (temCategoria) {
            return topicoRepo.findByCategoria(categoria, pageable);
        } else {
            return topicoRepo.findAll(pageable);
        }
    }

    // ── Detalhe ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TopicoForum buscarTopicoPorId(Long id) {
        return topicoRepo.findByIdWithRespostas(id)
                .orElseThrow(() -> new RuntimeException("Tópico não encontrado."));
    }

    @Transactional
    public void incrementarVisualizacoes(Long topicoId) {
        topicoRepo.incrementarVisualizacoes(topicoId);
    }

    @Transactional(readOnly = true)
    public Set<Long> getRespostasLikedByUser(Long topicoId, Long clienteId) {
        if (clienteId == null)
            return Collections.emptySet();
        return respostaRepo.findRespostaIdsLikedByClienteInTopico(topicoId, clienteId);
    }

    // ── Criação ───────────────────────────────────────────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public TopicoForum criarTopico(NovoTopicoDTO dto, Long autorId) {
        Cliente autor = clienteRepo.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        TopicoForum topico = new TopicoForum();
        topico.setTitulo(dto.getTitulo().trim());
        topico.setConteudo(dto.getConteudo().trim());
        topico.setCategoria(dto.getCategoria());
        topico.setAutor(autor);

        return topicoRepo.save(topico);
    }

    @SuppressWarnings("null")
    @Transactional
    public RespostaForum criarResposta(Long topicoId, NovaRespostaDTO dto, Long autorId) {
        TopicoForum topico = topicoRepo.findById(topicoId)
                .orElseThrow(() -> new RuntimeException("Tópico não encontrado."));
        Cliente autor = clienteRepo.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        RespostaForum resposta = new RespostaForum();
        resposta.setConteudo(dto.getConteudo().trim());
        resposta.setAutor(autor);
        resposta.setTopico(topico);

        RespostaForum salva = respostaRepo.save(resposta);

        topico.setQtdRespostas(topico.getQtdRespostas() + 1);
        topicoRepo.save(topico);

        return salva;
    }

    // ── Moderação ─────────────────────────────────────────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public void deletarTopico(Long id) {
        topicoRepo.deleteById(id);
    }

    @SuppressWarnings("null")
    @Transactional
    public void deletarResposta(Long id) {
        RespostaForum resposta = respostaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada."));
        TopicoForum topico = resposta.getTopico();
        respostaRepo.delete(resposta);
        if (topico.getQtdRespostas() > 0) {
            topico.setQtdRespostas(topico.getQtdRespostas() - 1);
            topicoRepo.save(topico);
        }
    }

    // ── Curtidas ──────────────────────────────────────────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public Map<String, Object> curtirResposta(Long respostaId, Long clienteId) {
        RespostaForum resposta = respostaRepo.findById(respostaId)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada."));

        boolean jaLikeu = resposta.getCurtidoresIds().contains(clienteId);
        if (jaLikeu) {
            resposta.getCurtidoresIds().remove(clienteId);
            resposta.setQtdCurtidas(Math.max(0, resposta.getQtdCurtidas() - 1));
        } else {
            resposta.getCurtidoresIds().add(clienteId);
            resposta.setQtdCurtidas(resposta.getQtdCurtidas() + 1);
        }
        respostaRepo.save(resposta);

        return Map.of(
                "curtidas", resposta.getQtdCurtidas(),
                "liked", !jaLikeu);
    }

    // ── Melhor Resposta ───────────────────────────────────────────────────────

    @SuppressWarnings("null")
    @Transactional
    public void marcarMelhorResposta(Long respostaId, Long clienteId, boolean isAdmin) {
        RespostaForum resposta = respostaRepo.findById(respostaId)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada."));
        TopicoForum topico = resposta.getTopico();

        if (!isAdmin && !topico.getAutor().getId().equals(clienteId)) {
            throw new RuntimeException("Apenas o autor do tópico pode marcar a melhor resposta.");
        }

        // Remove marcação anterior (se houver)
        respostaRepo.findByTopicoAndMelhorRespostaTrue(topico).ifPresent(prev -> {
            if (!prev.getId().equals(respostaId)) {
                prev.setMelhorResposta(false);
                respostaRepo.save(prev);
            }
        });

        boolean novoEstado = !resposta.isMelhorResposta();
        resposta.setMelhorResposta(novoEstado);
        respostaRepo.save(resposta);

        topico.setResolvido(novoEstado);
        topicoRepo.save(topico);
    }
}
