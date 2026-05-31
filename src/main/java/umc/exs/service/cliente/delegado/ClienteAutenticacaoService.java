package umc.exs.service.cliente.delegado;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.model.entidades.logic.RecuperacaoSenha;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.RecuperacaoSenhaRepository;
import umc.exs.service.cliente.senha.SenhaService;

@Service
@RequiredArgsConstructor
public class ClienteAutenticacaoService {

        private final ClienteRepositoryService repositoryService;
        private final PasswordEncoder passwordEncoder;

        private final RecuperacaoSenhaRepository tokenRepository;
        private final SenhaService senhaService;

        @Transactional
        public Cliente autenticar(
                        String email,
                        String senha) {

                Cliente cliente = repositoryService.encontrarPorEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "E-mail ou senha inválidos."));

                if (!cliente.isEmailVerificado()) {
                        throw new IllegalArgumentException(
                                        "E-mail não verificado.");
                }

                if (cliente.isBloqueada()) {
                        throw new IllegalArgumentException(
                                        "Conta bloqueada.");
                }

                if (!passwordEncoder.matches(
                                senha,
                                cliente.getSenha())) {

                        repositoryService.registrarFalhaLogin(cliente);

                        int restantes = Math.max(0, 5 - cliente.getTentativas());

                        throw new IllegalArgumentException(
                                        "Senha incorreta. Restam "
                                                        + restantes
                                                        + " tentativa(s).");
                }

                repositoryService.resetarTentativasLogin(cliente);

                return cliente;
        }

        public void gerarTokenRecuperacao(
                        Cliente cliente) {

                senhaService.iniciarRecuperacao(cliente);
        }

        @Transactional
        public Cliente redefinirSenha(
                        String token,
                        String novaSenha) {

                RecuperacaoSenha registro = tokenRepository.findByToken(token)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Token inválido."));

                if (registro.isExpirado()) {

                        tokenRepository.delete(registro);

                        throw new IllegalArgumentException(
                                        "Token expirado.");
                }

                Cliente cliente = registro.getCliente();

                cliente.setSenha(
                                passwordEncoder.encode(novaSenha));

                cliente.setBloqueada(false);
                cliente.setTentativas(0);

                repositoryService.salvar(cliente);

                tokenRepository.delete(registro);

                return cliente;
        }

        @Transactional
        public void alterarSenha(
                        String email,
                        String senhaAtual,
                        String novaSenha) {

                Cliente cliente = repositoryService.buscarPorEmailOuFalhar(email);

                if (!passwordEncoder.matches(
                                senhaAtual,
                                cliente.getSenha())) {

                        throw new IllegalArgumentException(
                                        "Senha atual incorreta.");
                }

                cliente.setSenha(
                                passwordEncoder.encode(novaSenha));

                repositoryService.salvar(cliente);
        }

        public boolean validarToken(String token) {

                return tokenRepository.findByToken(token)
                                .map(t -> !t.isExpirado())
                                .orElse(false);
        }
}
