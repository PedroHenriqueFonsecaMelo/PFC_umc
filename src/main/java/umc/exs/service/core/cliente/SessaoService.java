package umc.exs.service.core.cliente;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.SessaoAtiva;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.foundation.SessaoAtivaRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessaoService {

    private final SessaoAtivaRepository sessaoAtivaRepository;

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    @Transactional
    public SessaoAtiva registrarSessao(Cliente cliente, String token, String ip, String userAgent) {
        String hash = hashToken(token);
        SessaoAtiva sessao = SessaoAtiva.builder()
                .cliente(cliente)
                .tokenHash(hash)
                .dataLogin(LocalDateTime.now())
                .ip(ip)
                .userAgent(userAgent)
                .build();
        return sessaoAtivaRepository.save(sessao);
    }

    @Transactional
    public void encerrarSessao(String token) {
        String hash = hashToken(token);
        sessaoAtivaRepository.findByTokenHashAndAtivaTrue(hash).ifPresent(sessao -> {
            sessao.setAtiva(false);
            sessao.setDataLogout(LocalDateTime.now());
            sessaoAtivaRepository.save(sessao);
        });
    }

    @Transactional
    public void encerrarSessaoPorId(Long sessaoId, Long clienteId) {
        SessaoAtiva sessao = sessaoAtivaRepository.findById(sessaoId)
                .orElseThrow(() -> new RuntimeException("Sessão não encontrada."));
        if (!sessao.getCliente().getId().equals(clienteId)) {
            throw new IllegalArgumentException("Acesso negado: esta sessão não pertence ao seu perfil.");
        }
        sessao.setAtiva(false);
        sessao.setDataLogout(LocalDateTime.now());
        sessaoAtivaRepository.save(sessao);
        log.info("Sessão ID {} encerrada remotamente pelo cliente ID {}.", sessaoId, clienteId);
    }

    @Transactional
    public void encerrarTodasSessoes(Long clienteId) {
        List<SessaoAtiva> ativas = sessaoAtivaRepository.findByClienteIdAndAtivaTrue(clienteId);
        LocalDateTime agora = LocalDateTime.now();
        for (SessaoAtiva sessao : ativas) {
            sessao.setAtiva(false);
            sessao.setDataLogout(agora);
        }
        sessaoAtivaRepository.saveAll(ativas);
        log.info("{} sessão(ões) encerrada(s) para o cliente ID {}.", ativas.size(), clienteId);
    }

    public boolean isSessaoValida(String token) {
        String hash = hashToken(token);
        return sessaoAtivaRepository.findByTokenHashAndAtivaTrue(hash).isPresent();
    }

    public List<SessaoAtiva> listarSessoesAtivas(Long clienteId) {
        return sessaoAtivaRepository.findByClienteIdAndAtivaTrue(clienteId);
    }

    public List<SessaoAtiva> listarTodasSessoes(Long clienteId) {
        return sessaoAtivaRepository.findByClienteId(clienteId);
    }
}
