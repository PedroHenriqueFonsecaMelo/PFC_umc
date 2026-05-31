package umc.exs.service.cliente;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import umc.exs.dto.request.cliente.SignupRequest;
import umc.exs.model.entidades.foundation.EmailVerificacao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.foundation.EmailVerificacaoRepository;
import umc.exs.service.carteira.CarteiraService;
import umc.exs.service.cliente.delegado.ClienteAutenticacaoService;
import umc.exs.service.cliente.delegado.ClientePerfilService;
import umc.exs.service.cliente.delegado.ClienteRepositoryService;
import umc.exs.service.email.facade.EmailFacade;
import umc.exs.service.log.LogAuditoriaService;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

        @Mock
        ClienteRepositoryService repositoryService;

        @Mock
        ClientePerfilService perfilService;

        @Mock
        ClienteAutenticacaoService autenticacaoService;

        @Mock
        CarteiraService carteiraService;

        @Mock
        LogAuditoriaService auditoria;

        @Mock
        PasswordEncoder passwordEncoder;

        @Mock
        EmailVerificacaoRepository emailVerificacaoRepository;

        @Mock
        EmailFacade emailFacade;

        @InjectMocks
        ClienteService service;

        @Test
        void salvarCliente_quandoTermsNaoAceitos_deveLancar() {
                SignupRequest req = new SignupRequest();
                req.setTermsAccepted(false);
                req.setPrivacyAccepted(true);
                req.setEmail("a@b.com");

                assertThrows(IllegalArgumentException.class,
                                () -> service.salvarCliente(req));
        }

        @Test
        void salvarCliente_quandoOk_deveCadastrarEEnviaEmailVerificacao() {
                SignupRequest req = new SignupRequest();
                req.setTermsAccepted(true);
                req.setPrivacyAccepted(true);
                req.setEmail("user@test.com");
                req.setSenha("Senha@123");
                req.setNome("User");

                Cliente criado = new Cliente();
                criado.setId(1L);
                criado.setEmail("user@test.com");
                criado.setNome("User");

                when(repositoryService.existeEmailAtivo(anyString())).thenReturn(false);
                when(perfilService.cadastrar(any(SignupRequest.class))).thenReturn(criado);
                when(repositoryService.buscarPorId(1L)).thenReturn(criado);
                doNothing().when(emailVerificacaoRepository).deleteByClienteId(1L);
                when(emailVerificacaoRepository.save(any(EmailVerificacao.class)))
                                .thenAnswer(i -> i.getArgument(0));
                doNothing().when(emailFacade)
                                .sendHtmlSafe(anyString(), anyString(), anyString());

                Cliente res = service.salvarCliente(req);

                assertSame(criado, res);

                verify(auditoria).registrarLog(
                                eq("CADASTRO_USUARIO"),
                                eq(1L),
                                eq("user@test.com"),
                                anyString());

                verify(emailFacade).sendHtmlSafe(
                                eq("user@test.com"),
                                contains("Confirme seu e-mail"),
                                anyString());

                verify(emailVerificacaoRepository).save(any(EmailVerificacao.class));
        }

        @Test
        void uploadFotoPerfil_deveDelegarParaPerfilService() {
                MultipartFile file = mock(MultipartFile.class);

                when(perfilService.atualizarFoto(1L, file))
                                .thenReturn("/u.png");

                String res = service.uploadFotoPerfil(1L, file);

                assertEquals("/u.png", res);
                verify(perfilService).atualizarFoto(1L, file);
        }

        @Test
        void adicionarTokens_paraUsuarioLogado_deveChamarCarteiraService() {
                Cliente cliente = new Cliente();
                cliente.setId(10L);
                cliente.setEmail("c@test.com");

                when(repositoryService.buscarPorEmailOuFalhar("c@test.com"))
                                .thenReturn(cliente);

                doNothing().when(carteiraService)
                                .adicionarTokens(eq(cliente), eq(5.0), eq("PIX"), anyString());

                service.adicionarTokensParaUsuarioLogado("c@test.com", 5.0);

                verify(carteiraService).adicionarTokens(
                                eq(cliente),
                                eq(5.0),
                                eq("PIX"),
                                contains("Recarga"));
        }

        @Test
        void validarAtualizacao_deveLancarNomeObrigatorio() {
                assertThrows(IllegalArgumentException.class,
                                () -> service.validarAtualizacao(" ", "a"));
        }

        @Test
        void selecionarEnderecoParaUsuarioLogado_quandoNaoPertence_deveLancar() {
                Cliente cliente = new Cliente();
                cliente.setEnderecos(Set.of());
                cliente.setEnderecoSelecionadoId(null);

                when(repositoryService.buscarPorEmailOuFalhar("c@test.com"))
                                .thenReturn(cliente);

                assertThrows(IllegalArgumentException.class,
                                () -> service.selecionarEnderecoParaUsuarioLogado("c@test.com", 99L));
        }

        @Test
        void deletarContaPropria_deveAnonimizarESalvar() {
                Cliente cliente = new Cliente();
                cliente.setId(1L);
                cliente.setEmail("orig@test.com");
                cliente.setEnderecos(new HashSet<>(Set.of(new Endereco())));

                when(repositoryService.buscarPorEmailOuFalhar("orig@test.com"))
                                .thenReturn(cliente);

                when(repositoryService.salvar(any(Cliente.class)))
                                .thenReturn(cliente);

                service.deletarContaPropria("orig@test.com");

                verify(repositoryService).salvar(any(Cliente.class));

                assertNotEquals("orig@test.com", cliente.getEmail());
                assertEquals("Usuário Excluído", cliente.getNome());
                assertFalse(cliente.isAtivo());
        }
}