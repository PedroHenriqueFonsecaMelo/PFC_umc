package umc.exs.config.datainit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.model.enums.Genero;
import umc.exs.model.enums.StatusEnvio;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.log.LogAuditoriaService;

@Slf4j
@Service
@RequiredArgsConstructor
public class StressSeedService {

    private final AdminRepository adminRepo;
    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final LivroRepository livroRepo;
    private final LoteRepository loteRepo;
    private final PedidoRepository pedidoRepo;
    private final TopicoForumRepository topicoRepo;
    private final PasswordEncoder encoder;
    private final LogAuditoriaService auditoriaService; // Nome ajustado de acordo com seu código

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

    /**
     * Método auxiliar para simular autenticação na Thread atual do Seed
     */
    private void autenticarNoContexto(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null,
                java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Método orquestrador principal (Sem @Transactional para evitar Rollback total
     * caso a carga massiva falhe)
     */
    public void run() {

        if (!"create-drop".equalsIgnoreCase(ddlAuto) && !"create".equalsIgnoreCase(ddlAuto)) {
            log.info(
                    "Seed cancelado: ddl-auto está definido como '{}'. O StressSeed só roda em modo de recriação (create/create-drop).",
                    ddlAuto);
            return;
        }
        
        log.info("Iniciando execução do StressSeedService...");

        // 1. Garante a criação do Admin em um escopo transacional isolado e imediato
        try {
            salvarAdminIsolado();
        } catch (Exception e) {
            log.error("Erro crítico ao tentar salvar o Administrador Master: ", e);
            return; // Se não conseguir criar/validar o admin, interrompe a execução
        }

        // 2. Tenta executar a carga em massa de dados de teste (com sua própria
        // transação)
        try {
            executarCargaEmMassa();
        } catch (Exception e) {
            log.error(
                    "Ocorreu um erro durante a carga em massa dos dados de estresse. O Admin foi preservado, mas a carga falhou: ",
                    e);
        }
    }

    /**
     * Insere o Admin e commita imediatamente no Postgres
     */
    @Transactional
    public void salvarAdminIsolado() {
        // Validação correta pelo e-mail do Admin para não travar por conta dos clientes
        // antigos
        if (adminRepo.existsByEmail(admin_email)) {
            log.info("Administrador Master com o e-mail [{}] já existe. Pulando etapa do Admin.", admin_email);
            return;
        }

        log.info("Criando Administrador Master inicial...");
        Administrador admin = new Administrador();
        admin.setNome("Admin Master");
        admin.setEmail(admin_email);
        admin.setPassword(encoder.encode(admin_password));

        adminRepo.saveAndFlush(admin);
        log.info("🎯 Administrador Master gravado e commitado com sucesso no PostgreSQL!");
    }

    /**
     * Executa de forma atômica toda a carga volumétrica de dados
     */
    @Transactional
    public void executarCargaEmMassa() {
        if (clienteRepo.count() > 0) {
            log.info(
                    "A tabela de clientes já contém dados ({} registros). Carga em massa ignorada para evitar duplicações.",
                    clienteRepo.count());
            return;
        }

        // Recupera o admin criado para vinculá-lo nas auditorias e livros
        Administrador admin = adminRepo.findByEmail(admin_email)
                .orElseThrow(() -> new RuntimeException("Falha de integridade: Admin Master não encontrado no banco."));

        log.info("Iniciando seed de estresse temporariamente distribuído...");
        autenticarNoContexto(admin.getEmail());
        auditoriaService.registrarLog("STRESS_SEED_INICIADO", "Carga em massa executada por Admin Master");

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

        clientes = clienteRepo.saveAll(clientes);
        clienteRepo.flush();

        auditoriaService.registrarLog("CARGA_CLIENTES_SUCESSO",
                "Quantidade inserida: 2000 usuários distribuídos no tempo");

        // PONTUAÇÃO
        List<PontuacaoUsuario> pontuacoes = new ArrayList<>();
        for (Cliente c : clientes) {
            PontuacaoUsuario p = new PontuacaoUsuario();
            p.setCliente(c);
            p.setXpTotal(random.nextInt(500));
            p.setUltimaAtualizacao(c.getDataCriacao().plusDays(2));
            pontuacoes.add(p);
        }
        pontuacaoRepo.saveAll(pontuacoes);

        // LOTES
        List<Lote> lotes = new ArrayList<>();
        for (Cliente c : clientes.subList(0, 200)) {
            LocalDateTime dataLote = gerarDataCriacao();
            Lote lote = new Lote();
            lote.setCliente(c);
            lote.setCodigoProtocolo("LOTE-" + System.nanoTime());
            lote.setDataCriacao(dataLote);
            lote.setStatus(Lote.LoteStatus.TOTAL_APROVADO);
            lotes.add(lote);
        }
        lotes = loteRepo.saveAll(lotes);
        auditoriaService.registrarLog("CARGA_LOTES_SUCESSO",
                "Quantidade inserida: 200 lotes sob a conta de clientes simulados");

        // LIVROS
        List<Livro> livros = new ArrayList<>();
        String[] generosDisponiveis = { "Ficção", "Fantasia", "Clássico", "Distopia", "Terror" };

        for (int i = 0; i < 500; i++) {
            LocalDateTime dataAnuncio = gerarDataCriacao();
            Livro l = new Livro();

            String generoSorteado = generosDisponiveis[i % generosDisponiveis.length];
            l.setGenero(generoSorteado);
            l.setTitulo("Livro " + i);
            l.setAutor("Autor " + i);

            // Tratamento explícito para gerar ISBN estritamente numérico limpo para evitar
            // quebras de constraint textuais
            l.setIsbn(String.valueOf(ThreadLocalRandom.current().nextLong(1000000000000L, 9999999999999L)));

            l.setDataAnuncio(dataAnuncio);
            l.setPrecoAprovado(gerarSaldo() / 5.0);
            l.setEstadoAprovado(EstadoLivro.NOVO);
            l.setAprovado(true);
            l.setDataAprovacao(dataAnuncio.plusHours(random.nextInt(24)));
            l.setAdminAprovadorId(admin.getId());

            if (i < lotes.size()) {
                l.setLote(lotes.get(i));
            }
            livros.add(l);
        }

        livros = livroRepo.saveAll(livros);
        livroRepo.flush();
        auditoriaService.registrarLog("CARGA_LIVROS_SUCESSO",
                "Quantidade inserida: 500 livros aprovados distribuídos de forma retroativa");

        // TÓPICOS DO FÓRUM
        List<TopicoForum> topicos = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            Cliente autorAleatorio = clientes.get(random.nextInt(clientes.size()));

            TopicoForum t = new TopicoForum();
            t.setTitulo("Discussão sobre Literatura Geral nº " + i);
            t.setConteudo("Conteúdo expandido do tópico simulado de número " + i);
            t.setAutor(autorAleatorio);
            t.setCategoria(CategoriaForum.GERAL);
            t.setDataCriacao(gerarDataCriacao());

            topicos.add(t);
        }
        topicoRepo.saveAll(topicos);
        auditoriaService.registrarLog("CARGA_FORUM_SUCESSO",
                "Quantidade inserida: 300 tópicos criados de forma randômica por IDs de clientes");

        // PEDIDOS
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Cliente comprador = clientes.get(random.nextInt(clientes.size()));
            Livro livroComprado = livros.get(random.nextInt(livros.size()));
            LocalDateTime dataCompra = gerarDataCriacao();

            Pedido pedido = Pedido.builder()
                    .comprador(comprador)
                    .livroId(livroComprado.getId())
                    .tituloLivro(livroComprado.getTitulo())
                    .autorLivro(livroComprado.getAutor())
                    .isbnLivro(livroComprado.getIsbn())
                    .precoLivro(livroComprado.getPrecoAprovado())
                    .statusEnvio(StatusEnvio.ENTREGUE)
                    .dataCompra(dataCompra)
                    .dataAtualizacaoStatus(dataCompra.plusDays(ThreadLocalRandom.current().nextInt(2, 7)))
                    .codigoPedido(
                            "BIB-" + dataCompra.getYear() + "-" + ThreadLocalRandom.current().nextInt(10000, 99999))
                    .dataRetencaoExpira(dataCompra.toLocalDate().plusYears(5))
                    .build();

            pedidos.add(pedido);
        }

        pedidoRepo.saveAll(pedidos);
        auditoriaService.registrarLog("CARGA_PEDIDOS_SUCESSO",
                "Quantidade inserida: 1000 vendas finalizadas mapeadas com sucesso para o Dashboard");

        livroRepo.flush();
        pedidoRepo.flush();
        clienteRepo.flush();
        adminRepo.flush();

        auditoriaService.registrarLog("STRESS_SEED_FINALIZADO", "Banco de dados emulado em estresse pronto para uso.");
        SecurityContextHolder.clearContext();

        log.info("⚠️ SEED DE ESTRESSE COMPLETO E CORRIGIDO FINALIZADO COM SUCESSO ⚠️");
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
        LocalDateTime agora = LocalDateTime.now();

        int mesesParaSubtrair = ThreadLocalRandom.current().nextInt(24);
        LocalDateTime dataComMesSorteado = agora.minusMonths(mesesParaSubtrair);

        YearMonth anoMesSorteado = YearMonth.from(dataComMesSorteado);
        int maxDiasNoMes = anoMesSorteado.lengthOfMonth();

        int diaAleatorio = ThreadLocalRandom.current().nextInt(1, maxDiasNoMes + 1);

        int horaAleatoria = ThreadLocalRandom.current().nextInt(24);
        int minutoAleatorio = ThreadLocalRandom.current().nextInt(60);
        int segundoAleatorio = ThreadLocalRandom.current().nextInt(60);

        LocalDateTime dataGerada = dataComMesSorteado
                .withDayOfMonth(diaAleatorio)
                .withHour(horaAleatoria)
                .withMinute(minutoAleatorio)
                .withSecond(segundoAleatorio);

        if (dataGerada.isAfter(agora)) {
            return agora.minusDays(ThreadLocalRandom.current().nextInt(1, 28));
        }

        return dataGerada;
    }
}