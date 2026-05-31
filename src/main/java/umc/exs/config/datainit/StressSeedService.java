package umc.exs.config.datainit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.Genero;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.scheduler.PontosSchedulerService;

@Slf4j
@Service
@RequiredArgsConstructor
public class StressSeedService {

    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final LivroRepository livroRepo;
    private final LoteRepository loteRepo;
    private final TopicoForumRepository topicoRepo;
    private final PasswordEncoder encoder;
    private final PontosSchedulerService pontosSchedulerService;

    private final Random random = new Random();

    public void run() {

        if (clienteRepo.count() > 0) {
            log.info("Stress seed já rodou, ignorando.");
            return;
        }

        log.info(" Iniciando STRESS SEED: 5000 usuários, livros, forum...");

        List<Cliente> clientes = new ArrayList<>();
        String senhaCriptografada = encoder.encode("123");

        // 1 Criando 5000 usuários
        for (int i = 1; i <= 5000; i++) {
            Cliente c = new Cliente();
            c.setNome("Usuario " + i);
            c.setEmail("user" + i + "@stress.com");
            c.setSenha(senhaCriptografada);
            c.setCpf(String.format("900000%04d", i)); // CPF fictício
            c.setSaldoTokens(100.0 + random.nextInt(500));
            c.setEmailVerificado(true);
            c.setGen(i % 2 == 0 ? Genero.M : Genero.F);
            c.setDataCriacao(LocalDateTime.now().minusDays(random.nextInt(365)));
            clientes.add(c);
        }
        clienteRepo.saveAll(clientes);
        log.info(" 5000 usuários criados");

        // 2 Criando pontuações aleatórias
        List<PontuacaoUsuario> pontuacoes = new ArrayList<>();
        for (Cliente c : clientes) {
            int xpAprov = random.nextInt(1000);
            int xpComp = random.nextInt(500);
            int xpAval = random.nextInt(300);

            PontuacaoUsuario p = new PontuacaoUsuario();
            p.setCliente(c);
            p.setXpLivrosAprovados(xpAprov);
            p.setXpCompras(xpComp);
            p.setXpAvaliacoes(xpAval);
            p.setXpTotal(xpAprov + xpComp + xpAval);
            p.setUltimaAtualizacao(LocalDateTime.now().minusDays(random.nextInt(90)));
            p.setDataExpiracao(LocalDateTime.now().plusDays(45));
            pontuacoes.add(p);
        }
        pontuacaoRepo.saveAll(pontuacoes);
        log.info(" Pontuações aleatórias geradas");

        // 3 Criando lotes e livros
        List<Lote> lotes = new ArrayList<>();
        List<Livro> livros = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            Cliente cliente = clientes.get(random.nextInt(clientes.size()));

            Lote lote = new Lote();
            lote.setCliente(cliente);
            lote.setCodigoProtocolo("PROT-STRESS-" + i);
            lote.setStatus(Lote.LoteStatus.PENDENTE);
            lote.setDataCriacao(LocalDateTime.now().minusDays(random.nextInt(100)));
            lotes.add(lote);
        }
        loteRepo.saveAll(lotes);
        log.info(" 1000 lotes criados");

        for (int i = 1; i <= 1000; i++) {
            Lote lote = lotes.get(random.nextInt(lotes.size()));
            Livro livro = new Livro();
            livro.setTitulo("Livro Stress " + i);
            livro.setAutor("Autor " + i);
            livro.setIsbn("978-0000000" + i);
            livro.setLote(lote);
            livro.setAprovado(random.nextBoolean());
            livro.setDataAnuncio(LocalDateTime.now().minusDays(random.nextInt(90)));
            livros.add(livro);
        }
        livroRepo.saveAll(livros);
        log.info(" 1000 livros criados");

        // 4 Fórum
        List<TopicoForum> topicos = new ArrayList<>();
        for (int i = 1; i <= 500; i++) {
            Cliente autor = clientes.get(random.nextInt(clientes.size()));
            TopicoForum t = new TopicoForum();
            t.setTitulo("Topico Stress " + i);
            t.setConteudo("Conteudo gerado automaticamente para teste " + i);
            t.setAutor(autor);
            t.setCategoria(CategoriaForum.GERAL);
            t.setDataCriacao(LocalDateTime.now().minusDays(random.nextInt(100)));
            topicos.add(t);
        }
        topicoRepo.saveAll(topicos);
        log.info(" 500 tópicos de fórum criados");

        // 5 Processar decay XP
        pontosSchedulerService.processarDecayXp();
        log.info(" Decay XP processado");

        log.info(" STRESS SEED concluído!");
    }
}
