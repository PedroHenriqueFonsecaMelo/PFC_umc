package umc.exs.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.Genero;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.scheduler.PontosSchedulerService;

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
    private final CupomRepository cupomRepo;
    private final PasswordEncoder encoder;
    private final PontosSchedulerService pontosSchedulerService;

    @Override
    public void run(String... args) {
        log.info("🔍 Verificando integridade dos dados iniciais...");

        try {
            if (adminRepo.count() > 0) {
                log.info("Banco de dados já populado.");
                return;
            }

            // 1. Administrador
            Administrador admin = new Administrador();
            admin.setNome("Admin Master");
            admin.setEmail("admin@admin.com");
            admin.setPassword(encoder.encode("admin123"));
            admin = adminRepo.save(admin);

            // 2. Clientes
            Cliente c1 = createAndSaveClient("Cliente Teste", "cliente@teste.com", "12345678900");
            Cliente c2 = createAndSaveClient("Maria Leitora", "maria@teste.com", "98765432100");
            Cliente c5 = createAndSaveClient("Ana Decaimento", "ana@teste.com", "22233344455");
            Cliente c6 = createAndSaveClient("Carlos Zerado", "carlos@teste.com", "33344455566");
            Cliente c7 = createAndSaveClient("Lucia Penalidade", "lucia@teste.com", "44455566677");

            // 3. Pontuações (Cenários para Decay de XP)
            savePontuacaoCompleta(c1, 50, 60, 40, LocalDateTime.now());
            savePontuacaoCompleta(c2, 30, 40, 20, LocalDateTime.now().minusDays(15));
            savePontuacaoCompleta(c5, 700, 650, 650, LocalDateTime.now().minusDays(35));
            savePontuacaoCompleta(c7, 500, 500, 500, LocalDateTime.now().minusDays(40));
            savePontuacaoCompleta(c6, 300, 300, 200, LocalDateTime.now().minusDays(46));

            log.info("🎫 Criando cupons de teste...");

            saveCupom("BEMVINDO10", null, 10.0, 100, "PROMOCIONAL");

            saveCupom("PRESENTE50", c1, 50.0, 1, "PROMOCIONAL");

            saveCupom("XP-LOYALTY", c2, 15.0, 1, "PONTUACAO");

            // 4. Fórum
            TopicoForum topico = new TopicoForum();
            topico.setTitulo("Dúvida sobre Machado de Assis");
            topico.setConteudo("Qual a melhor obra para começar?");
            topico.setAutor(c1);
            topico.setDataCriacao(LocalDateTime.now().minusDays(5));
            topico.setCategoria(CategoriaForum.GERAL);
            topicoRepo.save(topico);

            RespostaForum resposta = new RespostaForum();
            resposta.setConteudo("Recomendo 'Dom Casmurro'!");
            resposta.setAutor(c2);
            resposta.setTopico(topico);
            resposta.setMelhorResposta(true);
            resposta.setDataCriacao(LocalDateTime.now().minusDays(2));
            respostaRepo.save(resposta);

            // 5. Lote e Livros (Restaurados)
            Lote lote = Lote.builder()
                    .codigoProtocolo("PROT-2026-X")
                    .status(Lote.LoteStatus.PENDENTE)
                    .cliente(c1)
                    .dataCriacao(LocalDateTime.now())
                    .build();
            loteRepo.save(lote);

            // Salvando livros em análise e aprovados
            saveLivro("1984", "George Orwell", "9780060918111", lote, false, null, null);
            saveLivro("Dom Casmurro", "Machado de Assis", "9788535902778", null, true, 30.0, admin.getId());
            saveLivro("O Hobbit", "J.R.R. Tolkien", "9780547928227", null, true, 45.0, admin.getId());

            // 6. Forçar Processamento do Decay
            log.info("⏳ Processando decaimento de XP (Ana, Lucia e Carlos)...");
            pontosSchedulerService.processarDecayXp();

            log.info("✨ CARGA COMPLETA: Admin, Clientes, Fórum, Livros e XP Decay processados.");

        } catch (Exception e) {
            log.error("❌ Falha no DataInitializer: ", e);
        }
    }

    private Cliente createAndSaveClient(String nome, String email, String cpf) {
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setEmail(email);
        c.setSenha(encoder.encode("admin123"));
        c.setSaldoTokens(100.0);
        c.setCpf(cpf);
        c.setDatanasc(LocalDate.parse("1995-05-10"));
        c.setGen(Genero.M);
        c.setBloqueada(false);
        c.setDataCriacao(LocalDateTime.now());
        c.setEmailVerificado(true);
        return clienteRepo.save(c);
    }

    private void savePontuacaoCompleta(Cliente c, int xpAprov, int xpComp, int xpAval, LocalDateTime ultAtiv) {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(c);
        p.setXpLivrosAprovados(xpAprov);
        p.setXpCompras(xpComp);
        p.setXpAvaliacoes(xpAval);
        p.setXpTotal(xpAprov + xpComp + xpAval);
        p.setUltimaAtualizacao(ultAtiv);
        p.setDataExpiracao(ultAtiv.plusDays(45));
        pontuacaoRepo.save(p);
    }

    private void saveLivro(String tit, String aut, String isbn, Lote lote, boolean aprov, Double preco, Long adminId) {
        Livro livro = new Livro();
        livro.setTitulo(tit);
        livro.setAutor(aut);
        livro.setIsbn(isbn);
        livro.setLote(lote);
        livro.setAprovado(aprov);
        livro.setDataAnuncio(LocalDateTime.now());
        if (aprov) {
            livro.setPrecoAprovado(preco);
            livro.setEstadoAprovado(EstadoLivro.BOM);
            livro.setAdminAprovadorId(adminId);
            livro.setDataAprovacao(LocalDateTime.now());
        }
        livroRepo.save(livro);
    }

    private void saveCupom(String codigo, Cliente cliente, Double desconto, Integer max, String tipo) {
        Cupom cupom = Cupom.builder()
                .codigo(codigo)
                .cliente(cliente) // Se null, é público
                .percentualDesconto(desconto)
                .quantidadeMaxima(max)
                .quantidadeUsada(0)
                .tipo(tipo)
                .usado(false)
                .dataCriacao(LocalDateTime.now())
                .expiracao(LocalDateTime.now().plusDays(30))
                .build();
        cupomRepo.save(cupom);
    }
}