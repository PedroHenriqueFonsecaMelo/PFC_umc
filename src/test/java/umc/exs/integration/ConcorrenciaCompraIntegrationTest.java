package umc.exs.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.service.core.livros.LivroService;
import umc.exs.service.email.EmailServiceGmailAPI;
import umc.exs.service.email.EmailServiceSmtp;
import umc.exs.service.email.facade.EmailFacade;

@SpringBootTest
@ActiveProfiles("test")
class ConcorrenciaCompraIntegrationTest {

    @Autowired
    private LivroRepository livroRepo;

    @Autowired
    private ClienteRepository clienteRepo;

    @Autowired
    private LivroService livroService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @MockitoBean
    private EmailFacade emailFacade;

    @MockitoBean
    private EmailServiceGmailAPI emailService;

    @MockitoBean
    private EmailServiceSmtp emailServiceSmtp;

    /**
     * Limpa o banco de dados desabilitando temporariamente as FKs.
     * Baseado nos mapeamentos @Table fornecidos.
     */
    void limparBancoDeDados() {
        transactionTemplate.execute(status -> {
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();

            String[] tables = {
                    "lista_desejos", "lote_livros", "pedido", "transacao",
                    "visita_site", "admins", "log_auditoria", "recuperacao_senha",
                    "comentario_blog", "pontuacao_usuario", "post_blog",
                    "resposta_forum", "topico_forum", "users", "livro"
            };

            for (String table : tables) {
                entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
            }

            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            return null;
        });
    }

    @Test
    void duasComprasConcorrentesMesmoLivro_UmaSucedeOutraFalha() throws InterruptedException {
        // --- ARRANGE ---
        limparBancoDeDados();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<Exception> errorThread1 = new AtomicReference<>();
        AtomicReference<Exception> errorThread2 = new AtomicReference<>();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        // Setup de dados (Buyers e Livro)
        criarCliente("buyer1@test.com", "12345678901");
        criarCliente("buyer2@test.com", "98765432109");

        Livro sharedLivro = Livro.builder()
                .titulo("Hot Book")
                .aprovado(true)
                .precoAprovado(10.0)
                .autor("romeu")
                .build();
        sharedLivro = livroRepo.saveAndFlush(sharedLivro);
        final Long livroId = sharedLivro.getId();

        // --- ACT ---
        // Criamos as duas tarefas para o executor
        for (int i = 0; i < 2; i++) {
            final int threadIdx = i;
            String email = (i == 0) ? "buyer1@test.com" : "buyer2@test.com";

            executor.submit(() -> {
                try {
                    startLatch.await(); // Aguarda o sinal de largada
                    livroService.realizarCompra(livroId, email);
                } catch (Exception e) {
                    if (threadIdx == 0)
                        errorThread1.set(e);
                    else
                        errorThread2.set(e);
                } finally {
                    endLatch.countDown(); // Sinaliza que esta thread terminou
                }
            });
        }

        // Dada a largada simultânea
        startLatch.countDown();

        // Espera as threads terminarem por até 10 segundos
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "O teste travou ou demorou demais!");
        executor.shutdown();

        // --- ASSERT ---
        entityManager.clear(); // Importante para limpar o cache do Hibernate no teste

        Livro livroAtualizado = livroRepo.findById(livroId)
                .orElseThrow();

        Exception erro1 = errorThread1.get();
        Exception erro2 = errorThread2.get();

        // Livro continua existindo
        assertNotNull(livroAtualizado);

        // Mas deve estar indisponível
        assertFalse(
                livroAtualizado.getAprovado(),
                "O livro deveria estar indisponível após a compra");

        // Verificação de Lógica: Apenas uma thread deve falhar (XOR)
        boolean umaFalhou = (erro1 != null) ^ (erro2 != null);
        assertTrue(umaFalhou, "Uma das threads deveria ter falhado por concorrência!");

        // Verificação de Saldo Total (200 - 10 = 190)
        double saldoTotal = clienteRepo.findAll().stream()
                .mapToDouble(Cliente::getSaldoTokens).sum();
        assertEquals(190.0, saldoTotal, "O saldo total deveria ser 190 (apenas uma compra realizada)");
    }

    // Helper para simplificar o Arrange
    private Cliente criarCliente(String email, String cpf) {
        Endereco end = new Endereco();
        end.setCep("01001000");
        end.setRua("Rua Teste");
        end.setNumero("1");
        end.setCidade("São Paulo");
        end.setEstado("SP");
        end.setBairro("Centro");
        end.setPais("Brasil");

        Cliente c = new Cliente();
        c.setEmail(email);
        c.setSaldoTokens(100.0);
        c.setNome("User " + email);
        c.setCpf(cpf);
        c.setSenha("admin123");
        c.setDatanasc(LocalDate.parse("1990-01-01"));
        c.getEnderecos().add(end);
        return clienteRepo.saveAndFlush(c);
    }
}