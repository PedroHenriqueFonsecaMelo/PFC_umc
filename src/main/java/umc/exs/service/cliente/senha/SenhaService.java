package umc.exs.service.cliente.senha;

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
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.email.html.EmailHtmlBuilder;
import umc.exs.service.log.AcaoAuditoria;
import umc.exs.service.log.AppLogger;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SenhaService {

    private final RecuperacaoSenhaRepository recuperacaoSenhaRepository;
    private final ClienteRepository clienteRepository;
    private final EmailFacade emailFacade;
    private final PasswordEncoder passwordEncoder;
    private final AppLogger appLogger;

    @Value("${app.base-url:https://localhost:8443}")
    private String baseUrl;

    @Transactional
    public void iniciarRecuperacao(Cliente cliente) {

        recuperacaoSenhaRepository.deleteByCliente(cliente);

        String token = UUID.randomUUID().toString().replace("-", "");

        RecuperacaoSenha rec = new RecuperacaoSenha(
                token,
                cliente,
                LocalDateTime.now().plusMinutes(30));

        rec.setEmail(cliente.getEmail());

        recuperacaoSenhaRepository.save(rec);

        String link = baseUrl + "/clientes/reset-senha?token=" + token;

        try {

            emailFacade.sendHtmlSafe(
                    cliente.getEmail(),
                    "Redefinição de senha — Bibliotroca",
                    EmailHtmlBuilder.recuperacaoSenha(cliente.getNome(), link));

            appLogger.success(
                    AcaoAuditoria.RECUPERACAO_SENHA_SOLICITADA,
                    cliente.getId(),
                    cliente.getEmail(),
                    "Token de recuperação gerado e e-mail enviado");

            log.info(
                    "RECUPERACAO_SENHA_SOLICITADA clienteId={} email={}",
                    cliente.getId(),
                    cliente.getEmail());

        } catch (Exception e) {

            appLogger.error(
                    AcaoAuditoria.RECUPERACAO_SENHA_SOLICITADA,
                    cliente.getId(),
                    cliente.getEmail(),
                    "Falha ao enviar e-mail de recuperação");

            log.error(
                    "RECUPERACAO_SENHA_ERRO_ENVIO_EMAIL clienteId={} email={}",
                    cliente.getId(),
                    cliente.getEmail(),
                    e);

            throw new IllegalStateException(
                    "Erro ao enviar e-mail de recuperação. Tente novamente mais tarde.",
                    e);
        }
    }

    @Transactional
    public void alterarSenhaComToken(String token, String novaSenha) {

        RecuperacaoSenha rec = recuperacaoSenhaRepository.findByToken(token)
                .filter(r -> LocalDateTime.now().isBefore(r.getDataExpiracao()))
                .orElseThrow(() -> {

                    appLogger.error(
                            AcaoAuditoria.RECUPERACAO_SENHA_EXPIRADA,
                            null,
                            null,
                            "Token inválido ou expirado");

                    log.warn(
                            "RECUPERACAO_SENHA_TOKEN_INVALIDO token={}",
                            token);

                    return new IllegalArgumentException(
                            "Link de recuperação inválido ou expirado.");
                });

        Cliente cliente = rec.getCliente();

        cliente.setSenha(passwordEncoder.encode(novaSenha));

        clienteRepository.save(cliente);

        recuperacaoSenhaRepository.delete(rec);

        appLogger.success(
                AcaoAuditoria.RECUPERACAO_SENHA_CONCLUIDA,
                cliente.getId(),
                cliente.getEmail(),
                "Senha redefinida via token");

        log.info(
                "RECUPERACAO_SENHA_CONCLUIDA clienteId={} email={}",
                cliente.getId(),
                cliente.getEmail());
    }

    @Transactional(readOnly = true)
    public boolean isTokenValido(String token) {

        boolean valido = recuperacaoSenhaRepository.findByToken(token)
                .map(rec -> LocalDateTime.now().isBefore(rec.getDataExpiracao()))
                .orElse(false);

        if (!valido) {
            log.debug(
                    "RECUPERACAO_SENHA_TOKEN_INVALIDO token={}",
                    token);
        }

        return valido;
    }
}