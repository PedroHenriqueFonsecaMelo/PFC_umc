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
 * - XP:     150 (ATIVO - sem penalidade)
 * * 2. Maria Leitora
 * - E-mail: maria@teste.com
 * - Senha:  admin123
 * - Saldo:  200.0 Tokens
 * - XP:     90 (INATIVO 10 dias - sem penalidade ainda)
 * * 3. João Leitor
 * - E-mail: joao@teste.com
 * - Senha:  admin123
 * - Saldo:  100.0 Tokens
 * - XP:     300 (PENALIDADE 30 dias - começa a reduzir)
 * 
 * ========================================================================
 * 👑 USUÁRIOS PARA TESTE DE PENALIDADES DE GAMIFICAÇÃO
 * ========================================================================
 * * 4. Pedro Inativo
 * - E-mail: pedro@teste.com
 * - Senha:  admin123
 * - XP:     1200 (Nível OURO - ativo há 5 dias, sem penalidade)
 * * 5. Ana Decaimento
 * - E-mail: ana@teste.com
 * - Senha:  admin123
 * - XP:     2000 (PENALIDADE MÉDIA 35 dias - em redução ativa)
 * - Demonstra: redução gradual (35-30=5 dias, reductionRatio ≈ 0.64)
 * * 6. Carlos Zerado
 * - E-mail: carlos@teste.com
 * - Senha:  admin123
 * - XP:     800 (PENALIDADE MÁXIMA 45 dias - XP quase zerado)
 * - Demonstra: estado próximo ao zero (45 > 44)
 * * 7. Lucia Penalidade
 * - E-mail: lucia@teste.com
 * - Senha:  admin123
 * - XP:     1500 (PENALIDADE CRÍTICA 40 dias - forte redução)
 * - Demonstra: redução forte (40-30=10 dias, reductionRatio ≈ 0.29)
 * 
 * 📊 CENÁRIOS TESTÁVEIS:
 * - XP Decay sem penalidade (c1, c2, c4): Verificar que XP não muda
 * - XP Decay iniciando (c3): 30 dias - primeiro checkpoint
 * - XP Decay em andamento (c5): 35 dias - redução média
 * - XP Decay crítica (c7): 40 dias - redução forte
 * - XP Decay máxima (c6): 45+ dias - XP zerado
 * 
 * 💡 TESTE: Acesse /api/gamificacao/meu-perfil para ver a aplicação da penalidade
 * ========================================================================
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
            Cliente c1 = createAndSaveClient("Cliente Teste", "cliente@teste.com", "12345678900", 5000.0, LocalDateTime.now().minusDays(5));
            Cliente c2 = createAndSaveClient("Maria Leitora", "maria@teste.com", "98765432100", 200.0, LocalDateTime.now().minusDays(15));
            Cliente c3 = createAndSaveClient("João Leitor", "joao@teste.com", "98765432101", 100.0, LocalDateTime.now().plusDays(30));
            
            // Clientes para testes de penalidades
            Cliente c4 = createAndSaveClient("Pedro Inativo", "pedro@teste.com", "11122233344", 1500.0, LocalDateTime.now().minusDays(10));
            Cliente c5 = createAndSaveClient("Ana Decaimento", "ana@teste.com", "22233344455", 2000.0, LocalDateTime.now().minusDays(35));
            Cliente c6 = createAndSaveClient("Carlos Zerado", "carlos@teste.com", "33344455566", 800.0, LocalDateTime.now().minusDays(45));
            Cliente c7 = createAndSaveClient("Lucia Penalidade", "lucia@teste.com", "44455566677", 1200.0, LocalDateTime.now().minusDays(40));
            
            log.info("✅ Clientes criados.");

            // 3. Criar Pontuações (Gamificação)
            // ============================================================
            // 👑 TESTANDO DIFERENTES CENÁRIOS DE PENALIDADES
            // ============================================================
            
            // Usuário ATIVO (sem penalidade) - XP atualizado hoje
            savePontuacaoCompleta(c1, 150, 50, 60, 40, LocalDateTime.now());
            
            // Usuário INATIVO (10 dias) - Ainda sem penalidade, mas próximo
            savePontuacaoCompleta(c2, 90, 30, 40, 20, LocalDateTime.now().minusDays(10));
            
            // Usuário PENALIDADE LEVE (30 dias) - Começa penalidade
            savePontuacaoCompleta(c3, 300, 100, 100, 100, LocalDateTime.now().minusDays(30));
            
            // Usuário ATIVO OURO (200 XP recente, para atingir nível OURO)
            savePontuacaoCompleta(c4, 1200, 400, 400, 400, LocalDateTime.now().minusDays(5));
            
            // Usuário PENALIDADE MÉDIA (35 dias) - Redução em andamento
            savePontuacaoCompleta(c5, 2000, 700, 650, 650, LocalDateTime.now().minusDays(35));
            
            // Usuário PENALIDADE MÁXIMA (45 dias) - XP já zerado ou próximo
            savePontuacaoCompleta(c6, 800, 300, 300, 200, LocalDateTime.now().minusDays(45));
            
            // Usuário PENALIDADE CRÍTICA (40 dias) - Quase zerado
            savePontuacaoCompleta(c7, 1500, 500, 500, 500, LocalDateTime.now().minusDays(40));

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

    private Cliente createAndSaveClient(String nome, String email, String cpf, Double saldo, LocalDateTime ultimaRecarga) {
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
        c.setEmailVerificado(true); // Clientes de teste já verificados para facilitar testes
        return clienteRepo.save(c);
    }

    private void savePontuacao(Cliente c, int xp) {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(c);
        p.setXpTotal(xp);
        p.setUltimaAtualizacao(LocalDateTime.now());
        pontuacaoRepo.save(p);
    }

    private void savePontuacaoCompleta(Cliente c, int xpTotal, int xpAprovacao, int xpCompra, int xpAvaliacao, LocalDateTime ultimaAtualizacao) {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(c);
        p.setXpTotal(xpTotal);
        p.setXpLivrosAprovados(xpAprovacao);  // XP de livros aprovados
        p.setXpCompras(xpCompra);             // XP de compras
        p.setXpAvaliacoes(xpAvaliacao);       // XP de avaliações
        p.setUltimaAtualizacao(ultimaAtualizacao);
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