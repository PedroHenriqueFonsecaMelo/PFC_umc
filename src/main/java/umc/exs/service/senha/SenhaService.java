package umc.exs.service.senha;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.email.EmailService;
import umc.exs.service.log.LogAuditoriaService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Responsabilidade: Ciclo de vida de segurança de credenciais,
 * geração de tokens de recuperação e redefinição de senhas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SenhaService {

    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private final LogAuditoriaService logAuditoriaService;

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    /**
     * Inicia o fluxo de esqueci minha senha.
     * Remove tokens antigos para evitar duplicidade e envia o link por e-mail.
     */
    @Transactional
    public void iniciarRecuperacao(Cliente cliente) {

        // Limpa solicitações anteriores do mesmo cliente
        recuperacaoSenhaRepository.deleteByCliente(cliente);

        String token = UUID.randomUUID().toString().replace("-", "");

        // Expiração em 30 minutos
        RecuperacaoSenha rec = new RecuperacaoSenha(token, cliente, LocalDateTime.now().plusMinutes(30));
        rec.setEmail(cliente.getEmail()); // Garante o preenchimento do campo email se existir na entidade

        recuperacaoSenhaRepository.save(rec);

        String link = baseUrl + "/clientes/reset-senha?token=" + token;

        try {
            emailService.enviar(cliente.getEmail(), "Recuperação de Senha",
                    "Olá, utilize o link a seguir para redefinir sua senha: " + link);
            log.info("E-mail de recuperação enviado com sucesso para {}", cliente.getEmail());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de recuperação para {}: {}", cliente.getEmail(), e.getMessage());
            throw new RuntimeException("Erro ao enviar e-mail de recuperação. Tente novamente mais tarde.");
        }
    }

    /**
     * Valida o token e atualiza a senha do cliente.
     */
    @Transactional
    public void alterarSenhaComToken(String token, String novaSenha) {
        log.debug("Tentativa de alteração de senha com token: {}", token);

        RecuperacaoSenha rec = recuperacaoSenhaRepository.findByToken(token)
                .filter(r -> LocalDateTime.now().isBefore(r.getDataExpiracao()))
                .orElseThrow(() -> {
                    log.warn("Tentativa de alteração de senha com token inválido ou expirado: {}", token);
                    return new IllegalArgumentException("Link de recuperação inválido ou expirado.");
                });

        Cliente cliente = rec.getCliente();

        // Criptografia da nova senha
        cliente.setSenha(passwordEncoder.encode(novaSenha));
        clienteRepository.save(cliente);

        logAuditoriaService.registrarLog("SENHA_ALTERADA", cliente.getId(), cliente.getEmail(),
                "Redefinida via token de recuperação");

        // Remove o token após o uso (segurança)
        recuperacaoSenhaRepository.delete(rec);

        log.info("Senha alterada com sucesso para o cliente: {}", cliente.getEmail());
    }

    /**
     * Apenas valida se o token ainda é útil (usado no carregamento da página de
     * reset).
     */
    @Transactional(readOnly = true)
    public boolean isTokenValido(String token) {
        return recuperacaoSenhaRepository.findByToken(token)
                .map(rec -> LocalDateTime.now().isBefore(rec.getDataExpiracao()))
                .orElse(false);
    }
}