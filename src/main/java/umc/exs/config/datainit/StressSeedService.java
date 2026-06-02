package umc.exs.config.datainit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.Genero;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class StressSeedService {

    private final AdminRepository adminRepo;
    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final LivroRepository livroRepo;
    private final LoteRepository loteRepo;
    private final TopicoForumRepository topicoRepo;
    private final PasswordEncoder encoder;

    private final Random random = new Random();

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    @Value("${ADMIN.EMAIL}")
    private String admin_email;

    @Value("${ADMIN.PASSWORD}")
    private String admin_password;

    private final String[] nomes = {
            "Juliana", "Andreia", "Diego", "Fabio", "Carlos",
            "Mariana", "Fernanda", "Ricardo", "Paulo", "Camila"
    };

    private final String[] sobrenomes = {
            "Lacerda", "Santana", "Nascimento", "Teixeira",
            "Silva", "Souza", "Oliveira", "Pereira"
    };

    private static final String SENHA_FIXA = "$2a$12$2NbprXL2fUcKB8wSYzsW1eCICV2PFsejTEZpXvJkb.GR0fbqNQbqa";

    @Transactional
    public void run() {

        if (!"create-drop".equalsIgnoreCase(ddlAuto) && !"create".equalsIgnoreCase(ddlAuto)) {
            log.info("Seed cancelado: ddl-auto está definido como '{}'. O StressSeed só roda em modo de recriação (create/create-drop).", ddlAuto);
            return;
        }

        if (clienteRepo.count() > 0) {
            log.info("Seed já executado.");
            return;
        }

        log.info("Iniciando seed...");

        // ADMIN
        Administrador admin = new Administrador();
        admin.setNome("Admin Master");
        admin.setEmail(admin_email);
        admin.setPassword(encoder.encode(admin_password));
        adminRepo.save(admin);

        // CLIENTES
        List<Cliente> clientes = new ArrayList<>();

        for (int i = 1; i <= 2000; i++) {

            String nomeCompleto = gerarNome();

            Cliente c = new Cliente();
            c.setNome(nomeCompleto);
            c.setEmail(gerarEmail(nomeCompleto, i));
            c.setSenha(SENHA_FIXA);
            c.setCpf(gerarCpf());
            c.setSaldoTokens(gerarSaldo());
            c.setDatanasc(gerarDataNascimento());
            c.setDataCriacao(gerarDataCriacao());
            c.setEmailVerificado(true);
            c.setGen(random.nextBoolean() ? Genero.M : Genero.F);
            c.setAtivo(true);
            c.setBloqueada(false);

            clientes.add(c);
        }

        List<Long> ids = clienteRepo.saveAll(clientes).stream()
                .map(Cliente::getId)
                .toList();
        clienteRepo.flush();

        // PONTUAÇÃO
        List<PontuacaoUsuario> pontuacoes = new ArrayList<>();

        for (Cliente c : clientes) {
            PontuacaoUsuario p = new PontuacaoUsuario();
            p.setCliente(c);
            p.setXpTotal(random.nextInt(500));
            p.setUltimaAtualizacao(LocalDateTime.now());
            pontuacoes.add(p);
        }

        pontuacaoRepo.saveAll(pontuacoes);

        // LOTES
        List<Lote> lotes = new ArrayList<>();

        for (Cliente c : clientes.subList(0, 200)) {
            Lote lote = new Lote();
            lote.setCliente(c);
            lote.setCodigoProtocolo("LOTE-" + System.nanoTime());
            lote.setDataCriacao(LocalDateTime.now());
            lotes.add(lote);
        }

        loteRepo.saveAll(lotes);

        // LIVROS
        List<Livro> livros = new ArrayList<>();

        for (int i = 0; i < 500; i++) {

            Livro l = new Livro();

            l.setTitulo("Livro " + i);
            l.setAutor("Autor " + i);
            l.setIsbn("ISBN-" + i);
            l.setGenero("Ficção");

            l.setDataAnuncio(LocalDateTime.now());
            l.setPrecoAprovado(gerarSaldo());
            l.setEstadoAprovado(EstadoLivro.NOVO);
            l.setAprovado(true);

            livros.add(l);
        }

        livroRepo.saveAll(livros);

        // TÓPICOS
        List<TopicoForum> topicos = new ArrayList<>();

        for (int i = 0; i < 300; i++) {

            TopicoForum t = new TopicoForum();
            t.setTitulo("Discussão " + i);
            t.setConteudo("Conteúdo do tópico " + i);
            t.setAutor(clienteRepo.findById(ids.get(random.nextInt(ids.size()))).orElse(null));
            t.setCategoria(CategoriaForum.GERAL);
            t.setDataCriacao(LocalDateTime.now());

            topicos.add(t);
        }

        topicoRepo.saveAll(topicos);

        log.info("SEED FINALIZADO");
    }

    private String gerarNome() {
        return nomes[random.nextInt(nomes.length)] + " " +
                sobrenomes[random.nextInt(sobrenomes.length)];
    }

    private String gerarEmail(String nome, int i) {
        return nome.toLowerCase().replace(" ", ".") + i + "@outlook.com";
    }

    private String gerarCpf() {
        return String.format("%03d.%03d.%03d-%02d",
                random.nextInt(1000),
                random.nextInt(1000),
                random.nextInt(1000),
                random.nextInt(100));
    }

    private double gerarSaldo() {
        double valor = random.nextDouble() * 500;
        return BigDecimal.valueOf(valor)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private LocalDate gerarDataNascimento() {
        return LocalDate.of(
                1960 + random.nextInt(40),
                1 + random.nextInt(12),
                1 + random.nextInt(28));
    }

    private LocalDateTime gerarDataCriacao() {
        return LocalDateTime.now().minusDays(random.nextInt(700));
    }
}