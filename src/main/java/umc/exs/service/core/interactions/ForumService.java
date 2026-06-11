package umc.exs.service.core.interactions;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.dto.request.cliente.NovoTopicoRequest;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.gamificacao.GamificacaoService;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

/**
 * Serviço responsável pelas operações do fórum da plataforma.
 * Gerencia tópicos, respostas, curtidas e a marcação de melhor resposta com gamificação.
 */
@Service
@RequiredArgsConstructor
public class ForumService {

    private final TopicoForumRepository topicoRepo;
    private final RespostaForumRepository respostaRepo;
    private final ClienteRepository clienteRepo;
    private final GamificacaoService gamificacaoService;
    private final AppLogger appLogger;

    /**
     * Lista os tópicos do fórum com suporte a filtro por busca textual e/ou categoria, de forma paginada.
     */
    @Transactional(readOnly = true)
    public Page<TopicoForum> listarTopicos(String busca, CategoriaForum categoria, Pageable pageable) {

        boolean temBusca = busca != null && !busca.isBlank();
        boolean temCategoria = categoria != null;

        // Aplica os filtros disponíveis: busca + categoria, apenas busca, apenas categoria ou nenhum
        if (temBusca && temCategoria) {
            return topicoRepo.findByTituloContainingIgnoreCaseAndCategoria(busca, categoria, pageable);
        }
        if (temBusca) {
            return topicoRepo.findByTituloContainingIgnoreCase(busca, pageable);
        }
        if (temCategoria) {
            return topicoRepo.findByCategoria(categoria, pageable);
        }

        return topicoRepo.findAll(pageable);
    }

    /**
     * Busca um tópico pelo ID, carregando também suas respostas.
     */
    @Transactional(readOnly = true)
    public TopicoForum buscarTopicoPorId(Long id) {
        return topicoRepo.findByIdWithRespostas(id)
                .orElseThrow(() -> new RuntimeException("Tópico não encontrado"));
    }

    /**
     * Incrementa o contador de visualizações de um tópico a cada acesso.
     */
    @Transactional
    public void incrementarVisualizacoes(Long topicoId) {
        topicoRepo.incrementarVisualizacoes(topicoId);

        appLogger.info(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "VIEW_TOPICO id=" + topicoId);
    }

    /**
     * Retorna os IDs das respostas curtidas pelo cliente em um determinado tópico.
     * Retorna conjunto vazio se o cliente não estiver autenticado.
     */
    @Transactional(readOnly = true)
    public Set<Long> getRespostasLikedByUser(Long topicoId, Long clienteId) {
        if (clienteId == null) {
            return Collections.emptySet();
        }
        return respostaRepo.findRespostaIdsLikedByClienteInTopico(topicoId, clienteId);
    }

    /**
     * Cria um novo tópico no fórum associado ao autor identificado pelo ID.
     */
    @Transactional
    public TopicoForum criarTopico(NovoTopicoRequest dto, Long autorId) {

        Cliente autor = clienteRepo.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        TopicoForum topico = new TopicoForum();
        topico.setTitulo(dto.getTitulo().trim());
        topico.setConteudo(dto.getConteudo().trim());
        topico.setCategoria(dto.getCategoria());
        topico.setAutor(autor);

        TopicoForum salvo = topicoRepo.save(topico);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                autorId,
                autor.getEmail(),
                "TOPICO_CRIADO id=" + salvo.getId());

        return salvo;
    }

    /**
     * Adiciona uma nova resposta a um tópico e incrementa o contador de respostas do tópico.
     */
    @Transactional
    public RespostaForum criarResposta(Long topicoId, String conteudo, Long autorId) {

        TopicoForum topico = topicoRepo.findById(topicoId)
                .orElseThrow(() -> new RuntimeException("Tópico não encontrado"));

        Cliente autor = clienteRepo.findById(autorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        RespostaForum resposta = new RespostaForum();
        resposta.setConteudo(conteudo.trim());
        resposta.setAutor(autor);
        resposta.setTopico(topico);

        RespostaForum salva = respostaRepo.save(resposta);

        // Atualiza o contador de respostas do tópico após salvar a nova resposta
        topico.setQtdRespostas(topico.getQtdRespostas() + 1);
        topicoRepo.save(topico);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                autorId,
                autor.getEmail(),
                "RESPOSTA_CRIADA topicoId=" + topicoId);

        return salva;
    }

    /**
     * Remove um tópico do fórum pelo ID.
     */
    @Transactional
    public void deletarTopico(Long id) {

        topicoRepo.deleteById(id);

        appLogger.error(
                AcaoAuditoria.GENERICO,
                null,
                null,
                "TOPICO_DELETADO id=" + id);
    }

    /**
     * Verifica se o usuário com o e-mail informado é o autor de uma resposta específica.
     */
    @Transactional(readOnly = true)
    public boolean isAutorResposta(Long respostaId, String emailUsuario) {

        return respostaRepo.findById(respostaId)
                .map(r -> r.getAutor() != null &&
                        r.getAutor().getEmail() != null &&
                        r.getAutor().getEmail().equals(emailUsuario))
                .orElse(false);
    }

    /**
     * Remove uma resposta do fórum e decrementa o contador de respostas do tópico correspondente.
     */
    @Transactional
    public void deletarResposta(Long id) {

        RespostaForum resposta = respostaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada"));

        TopicoForum topico = resposta.getTopico();

        respostaRepo.delete(resposta);

        // Garante que o contador não fique negativo ao decrementar
        if (topico.getQtdRespostas() > 0) {
            topico.setQtdRespostas(topico.getQtdRespostas() - 1);
            topicoRepo.save(topico);
        }

        appLogger.error(
                AcaoAuditoria.GENERICO,
                resposta.getAutor() != null ? resposta.getAutor().getId() : null,
                resposta.getAutor() != null ? resposta.getAutor().getEmail() : null,
                "RESPOSTA_DELETADA id=" + id);
    }

    /**
     * Alterna a curtida de uma resposta: adiciona se ainda não curtiu, remove se já curtiu.
     * Retorna o total de curtidas e o novo estado de like.
     */
    @Transactional
    public Map<String, Object> curtirResposta(Long respostaId, Long clienteId) {

        RespostaForum resposta = respostaRepo.findById(respostaId)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada"));

        // Verifica se o cliente já curtiu para fazer toggle (curtir/descurtir)
        boolean jaLikeu = resposta.getCurtidoresIds().contains(clienteId);

        if (jaLikeu) {
            resposta.getCurtidoresIds().remove(clienteId);
            resposta.setQtdCurtidas(Math.max(0, resposta.getQtdCurtidas() - 1));
        } else {
            resposta.getCurtidoresIds().add(clienteId);
            resposta.setQtdCurtidas(resposta.getQtdCurtidas() + 1);
        }

        respostaRepo.save(resposta);

        appLogger.info(
                AcaoAuditoria.GENERICO,
                clienteId,
                null,
                "LIKE_RESPOSTA respostaId=" + respostaId + " estado=" + (!jaLikeu));

        return Map.of(
                "curtidas", resposta.getQtdCurtidas(),
                "liked", !jaLikeu);
    }

    /**
     * Marca ou desmarca uma resposta como a melhor do tópico.
     * Apenas o autor do tópico ou um admin pode realizar essa ação; concede XP ao autor da resposta.
     */
    @Transactional
    public void marcarMelhorResposta(Long respostaId, Long clienteId, boolean isAdmin) {

        RespostaForum resposta = respostaRepo.findById(respostaId)
                .orElseThrow(() -> new RuntimeException("Resposta não encontrada"));

        TopicoForum topico = resposta.getTopico();

        // Valida que somente o autor do tópico ou o admin pode marcar a melhor resposta
        if (!isAdmin && !topico.getAutor().getId().equals(clienteId)) {
            throw new RuntimeException("Sem permissão");
        }

        // Remove a marcação da melhor resposta anterior, se existir e for diferente da atual
        respostaRepo.findByTopicoAndMelhorRespostaTrue(topico).ifPresent(prev -> {
            if (!prev.getId().equals(respostaId)) {
                prev.setMelhorResposta(false);
                respostaRepo.save(prev);
            }
        });

        boolean novoEstado = !resposta.isMelhorResposta();
        resposta.setMelhorResposta(novoEstado);
        respostaRepo.save(resposta);

        // Concede XP ao autor da resposta apenas quando a resposta é marcada como melhor
        if (novoEstado) {
            gamificacaoService.xpAvaliacao(resposta.getAutor().getId());
        }

        topico.setResolvido(novoEstado);
        topicoRepo.save(topico);

        appLogger.success(
                AcaoAuditoria.GENERICO,
                clienteId,
                null,
                "MELHOR_RESPOSTA respostaId=" + respostaId + " estado=" + novoEstado);
    }
}