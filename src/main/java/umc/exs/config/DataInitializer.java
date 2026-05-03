package umc.exs.config;

import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import umc.exs.model.entidades.foundation.*;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.Genero;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;

/*
 * ========================================================================
 * 🔑 CREDENCIAIS DE ACESSO (CRIADAS NESTE INICIALIZADOR)
 * ========================================================================
 * * 🛠️ ADMINISTRADOR (Acesso total via /admin/login):
 * - E-mail: admin@admin.com
 * - Senha:  admin123
 *
 * * 👤 CLIENTES (Acesso via /clientes/login):
 * * 1. Cliente Teste
 * - E-mail: cliente@teste.com
 * - Senha:  admin123
 * - Saldo:  5000.0 Tokens
 * * 2. Maria Leitora
 * - E-mail: maria@teste.com
 * - Senha:  admin123
 * - Saldo:  200.0 Tokens
 * * ========================================================================
 */

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepo;
    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final LoteRepository loteRepo;
    private final TopicoForumRepository topicoRepo;
    private final LivroRepository livroRepo;
    private final RespostaForumRepository respostaRepo;
    private final PasswordEncoder encoder;

    @SuppressWarnings("null")
    @Override
    public void run(String... args) {
        log.info("🔍 Verificando integridade dos dados iniciais...");

        try {
            // Se já tiver admin, assumimos que o banco já está pronto
            if (adminRepo.count() > 0) {
                log.info("ℹ️ Banco de dados já populado. Ignorando DataInitializer.");
                return;
            }

            log.info("🚀 Iniciando carga massiva de dados...");

            // 1. Criar Administrador
            Administrador admin = new Administrador();
            admin.setNome("Admin Master");
            admin.setEmail("admin@admin.com");
            admin.setPassword(encoder.encode("admin123"));
            admin = adminRepo.save(admin);
            log.info("✅ Admin criado.");

            // 2. Criar Clientes (Garante campos que costumam dar erro de NULL)
            Cliente c1 = createAndSaveClient("Cliente Teste", "cliente@teste.com", "12345678900", 5000.0);
            Cliente c2 = createAndSaveClient("Maria Leitora", "maria@teste.com", "98765432100", 200.0);
            log.info("✅ Clientes criados.");

            // 3. Criar Pontuações (Gamificação)
            savePontuacao(c1, 150);
            savePontuacao(c2, 90);

            // 4. Criar Lote de Livros (Necessário para LivroAnuncio que não é vitrine)
            Lote lote = Lote.builder()
                    .codigoProtocolo("PROT-2024-001")
                    .status(Lote.LoteStatus.PENDENTE)
                    .cliente(c1)
                    .dataCriacao(LocalDateTime.now())
                    .build();
            lote = loteRepo.save(lote);

            // 5. Salvar Livros
            // Livros em análise (com lote)
            saveLivro("1984", "George Orwell", "9780060918111", lote, false, null, null);

            // Livros já na Vitrine (aprovados por admin)
            saveLivro("Dom Casmurro", "Machado de Assis", "9788535902778", null, true, 30.0, admin.getId());
            saveLivro("Sapiens", "Yuval Noah Harari", "9780062316097", null, true, 50.0, admin.getId());
            log.info("✅ Livros e Vitrine prontos.");

            // 6. Fórum (Tópico e Resposta)
            TopicoForum topico = new TopicoForum();
            topico.setTitulo("Dúvida sobre Capitu");
            topico.setConteudo("Afinal, traiu ou não traiu?");
            topico.setAutor(c1);
            topico.setDataCriacao(LocalDateTime.now().minusDays(5));
            topico.setCategoria(CategoriaForum.GERAL);
            topico = topicoRepo.save(topico);

            RespostaForum resposta = new RespostaForum();
            resposta.setConteudo("O mistério é a melhor parte do livro!");
            resposta.setAutor(c2);
            resposta.setTopico(topico);
            resposta.setMelhorResposta(true);
            resposta.setDataCriacao(LocalDateTime.now().minusDays(2));
            respostaRepo.save(resposta);

            log.info("✨ CARGA DE DADOS FINALIZADA COM SUCESSO!");

        } catch (Exception e) {
            log.error("❌ Erro durante o DataInitializer: ", e);
            log.error("Causa do erro: {}", (e.getCause() != null ? e.getCause().getMessage() : "Desconhecida"));
        }
    }

    private Cliente createAndSaveClient(String nome, String email, String cpf, Double saldo) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setEmail(email);
        c.setSenha(encoder.encode("admin123"));
        c.setSaldoTokens(saldo);
        c.setCpf(cpf);
        c.setDatanasc("1990-01-01");
        c.setGen(Genero.M);
        c.setBloqueada(false);
        c.setDataCriacao(LocalDateTime.now());
        return clienteRepo.save(c);
    }

    private void savePontuacao(Cliente c, int xp) {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(c);
        p.setXpTotal(xp);
        p.setUltimaAtualizacao(LocalDateTime.now());
        pontuacaoRepo.save(p);
    }

    private void saveLivro(String titulo, String autor, String isbn, Lote lote, boolean aprovado, Double preco,
            Long adminId) {
        Livro livro = new Livro();
        livro.setTitulo(titulo);
        livro.setAutor(autor);
        livro.setIsbn(isbn);
        livro.setLote(lote);
        livro.setAprovado(aprovado);
        livro.setDataAnuncio(LocalDateTime.now());

        if (aprovado) {
            livro.setPrecoAprovado(preco);
            livro.setEstadoAprovado(EstadoLivro.BOM);
            livro.setAdminAprovadorId(adminId);
            livro.setDataAprovacao(LocalDateTime.now());
        }

        livroRepo.save(livro);
    }
}