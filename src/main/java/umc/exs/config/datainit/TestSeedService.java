package umc.exs.config.datainit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote;
import umc.exs.model.entidades.foundation.Pedido;
import umc.exs.model.entidades.livro.Livro;
import umc.exs.model.entidades.logic.Administrador;
import umc.exs.model.entidades.social.PontuacaoUsuario;
import umc.exs.model.entidades.social.RespostaForum;
import umc.exs.model.entidades.social.TopicoForum;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.CategoriaForum;
import umc.exs.model.enums.EstadoLivro;
import umc.exs.repository.livro.LivroRepository;
import umc.exs.repository.logic.AdminRepository;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.LoteRepository;
import umc.exs.repository.negocios.PedidoRepository;
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
import umc.exs.service.log.LogAuditoriaService;
import umc.exs.service.scheduler.PontosSchedulerService;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestSeedService {

    private final AdminRepository adminRepo;
    private final ClienteRepository clienteRepo;
    private final PontuacaoUsuarioRepository pontuacaoRepo;
    private final LivroRepository livroRepo;
    private final LoteRepository loteRepo;
    private final PedidoRepository pedidoRepo;
    private final TopicoForumRepository topicoRepo;
    private final RespostaForumRepository respostaRepo;
    private final CupomRepository cupomRepo;
    private final PasswordEncoder encoder;
    private final PontosSchedulerService pontosSchedulerService;
    private final LogAuditoriaService auditoriaService;

    public void run() {

        if (adminRepo.count() > 0)
            return;

        // ADMIN
        Administrador admin = new Administrador();
        admin.setNome("Admin Master");
        admin.setEmail("admin@admin.com");
        admin.setPassword(encoder.encode("admin123"));
        admin = adminRepo.save(admin);

        // Força o sistema a achar que o Admin está logado para os próximos logs
        autenticarNoContexto(admin.getEmail());
        auditoriaService.registrarLog("ADMIN_CRIADO", "Admin mestre gerado pelo sistema");

        // CLIENTES FIXOS (7)
        Cliente c1 = create("Cliente Teste", "cliente@teste.com", "12345678900");
        Cliente c2 = create("Maria Leitora", "maria@teste.com", "98765432100");
        Cliente c3 = create("Joao Teste", "joao@teste.com", "11122233344");
        Cliente c4 = create("Pedro Leitor", "pedro@teste.com", "55566677788");
        Cliente c5 = create("Ana Decaimento", "ana@teste.com", "22233344455");
        Cliente c6 = create("Carlos Zerado", "carlos@teste.com", "33344455566");
        Cliente c7 = create("Lucia Penalidade", "lucia@teste.com", "44455566677");

        saveXp(c1, 50, 60, 40);
        saveXp(c2, 30, 40, 20);
        saveXp(c3, 100, 50, 20);
        saveXp(c4, 10, 5, 0);
        saveXp(c5, 700, 650, 650);
        saveXp(c6, 300, 300, 200);
        saveXp(c7, 500, 500, 500);

        // CUPONS
        saveCupom("BEMVINDO10", c1);
        saveCupom("PRESENTE50", c1);
        saveCupom("XP-LOYALTY", c2);

        // FORUM
        autenticarNoContexto(c1.getEmail());
        TopicoForum t = new TopicoForum();
        t.setTitulo("Machado de Assis");
        t.setConteudo("Qual obra começar?");
        t.setAutor(c1);
        t.setCategoria(CategoriaForum.GERAL);
        t.setDataCriacao(gerarDataCriacao());
        topicoRepo.save(t);
        auditoriaService.registrarLog("TOPICO_FORUM_CRIADO", "Titulo: " + t.getTitulo());

        autenticarNoContexto(c2.getEmail());
        String conteudoResposta = "Dom Casmurro!";
        RespostaForum r = new RespostaForum();
        r.setConteudo(conteudoResposta);
        r.setAutor(c2);
        r.setTopico(t);
        respostaRepo.save(r);
        auditoriaService.registrarLog("RESPOSTA_FORUM_CRIADA", "Resposta no tópico ID: " + t.getId());

        // LIVROS
        autenticarNoContexto(c1.getEmail());
        Lote lote = new Lote();
        lote.setCliente(c1);
        lote.setCodigoProtocolo("PROT-TESTE");
        lote.setStatus(Lote.LoteStatus.PENDENTE);
        loteRepo.save(lote);
        auditoriaService.registrarLog("LOTE_ENVIADO", "Protocolo: " + lote.getCodigoProtocolo());

        // Salvando livro pendente em análise
        saveLivro("1984", "George Orwell", "9780060918111", "ficção", lote, false, null, null, gerarDataCriacao());

        // Livros aprovados pelo Admin (Capturando o retorno para associar aos pedidos)
        autenticarNoContexto(admin.getEmail());

        Livro livroDomCasmurro = saveLivro("Dom Casmurro", "Machado de Assis", "9788535902778", "aventura", null, true,
                30.0, admin.getId(), gerarDataCriacao());
        auditoriaService.registrarLog("LIVRO_APROVADO", "ISBN: 9788535902778, Titulo: Dom Casmurro");

        Livro livroHobbit = saveLivro("O Hobbit", "J.R.R. Tolkien", "9780547928227", "medieval", null, true, 45.0,
                admin.getId(), gerarDataCriacao());
        auditoriaService.registrarLog("LIVRO_APROVADO", "ISBN: 9780547928227, Titulo: O Hobbit");

        // VENDAS (PEDIDOS)
        // Sorteia datas passadas aleatórias para as vendas fictícias
        LocalDateTime dataVenda1 = gerarDataCriacao();
        LocalDateTime dataVenda2 = gerarDataCriacao();

        // Cliente 2 comprando o livro "Dom Casmurro"
        savePedido(c2, livroDomCasmurro, 30.0, dataVenda1);
        auditoriaService.registrarLog("PEDIDO_FINALIZADO", "Cliente 2 comprou Dom Casmurro");

        // Cliente 3 comprando "O Hobbit"
        savePedido(c3, livroHobbit, 45.0, dataVenda2);
        auditoriaService.registrarLog("PEDIDO_FINALIZADO", "Cliente 3 comprou O Hobbit");

        livroRepo.flush();
        pedidoRepo.flush();
        clienteRepo.flush();

        // Ações automáticas do sistema rodam como "SISTEMA"
        SecurityContextHolder.clearContext();
        pontosSchedulerService.processarDecayXp();

        log.info(" TEST SEED finalizado");
    }

    private void autenticarNoContexto(String email) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null,
                java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Cliente create(String nome, String email, String cpf) {
        int age = ThreadLocalRandom.current().nextInt(18, 61);
        Cliente c = new Cliente();
        c.setNome(nome);
        c.setEmail(email);
        c.setSenha(encoder.encode("admin123"));
        c.setDatanasc(LocalDate.now().minusYears(age));
        c.setCpf(cpf);
        c.setSaldoTokens(100.0);
        c.setEmailVerificado(true);
        c.setDataCriacao(gerarDataCriacao());

        Cliente salvo = clienteRepo.save(c);
        return salvo;
    }

    // Alterado para retornar "Livro" para permitir o fluxo de vendas do Pedido
    private Livro saveLivro(String tit, String aut, String isbn, String gen, Lote lote,
            boolean aprov, Double preco, Long adminId, LocalDateTime dataAleatoria) {

        Livro livro = new Livro();
        livro.setTitulo(tit);
        livro.setAutor(aut);
        livro.setIsbn(isbn);
        livro.setGenero(gen);
        livro.setLote(lote);
        livro.setAprovado(aprov);
        livro.setDataAnuncio(dataAleatoria);

        if (aprov) {
            livro.setPrecoAprovado(preco);
            livro.setEstadoAprovado(EstadoLivro.BOM);
            livro.setAdminAprovadorId(adminId);
            livro.setDataAprovacao(dataAleatoria.plusHours(2));
        }
        return livroRepo.save(livro);
    }

    private void saveXp(Cliente c, int a, int b, int c2) {
        PontuacaoUsuario p = new PontuacaoUsuario();
        p.setCliente(c);
        p.setXpLivrosAprovados(a);
        p.setXpCompras(b);
        p.setXpAvaliacoes(c2);
        p.setXpTotal(a + b + c2);
        p.setUltimaAtualizacao(LocalDateTime.now());
        pontuacaoRepo.save(p);
    }

    private void saveCupom(String code, Cliente c) {
        Cupom cupom = new Cupom();
        cupom.setCodigo(code);
        cupom.setCliente(c);
        cupom.setPercentualDesconto(10.0);
        cupom.setQuantidadeMaxima(100);
        cupom.setQuantidadeUsada(0);
        cupom.setUsado(false);
        cupom.setValorTokens(0.0);
        cupom.setTipo("PROMOCIONAL");
        cupom.setExpiracao(LocalDateTime.now().plusMonths(1));
        cupom.setDataCriacao(gerarDataCriacao());
        cupomRepo.save(cupom);
    }

    private void savePedido(Cliente comprador, Livro livro, Double preco, LocalDateTime dataAleatoria) {
        Pedido pedido = Pedido.builder()
                .comprador(comprador)
                .livroId(livro.getId())
                .tituloLivro(livro.getTitulo())
                .autorLivro(livro.getAutor())
                .isbnLivro(livro.getIsbn())
                .precoLivro(preco)
                .statusEnvio(umc.exs.model.enums.StatusEnvio.ENTREGUE)
                .dataCompra(dataAleatoria)
                .dataAtualizacaoStatus(dataAleatoria.plusDays(3))
                .codigoPedido("BIB-" + dataAleatoria.getYear() + "-" + ThreadLocalRandom.current().nextInt(1000, 9999))
                .build();

        pedidoRepo.save(pedido);
    }

    private LocalDateTime gerarDataCriacao() {
        int anoAleatorio = ThreadLocalRandom.current().nextInt(2024, 2027);
        int mesAleatorio = ThreadLocalRandom.current().nextInt(1, 13);

        YearMonth ym = YearMonth.of(anoAleatorio, mesAleatorio);
        int diaAleatorio = ThreadLocalRandom.current().nextInt(1, ym.lengthOfMonth() + 1);

        int hora = ThreadLocalRandom.current().nextInt(0, 24);
        int minuto = ThreadLocalRandom.current().nextInt(0, 60);
        int segundo = ThreadLocalRandom.current().nextInt(0, 60);

        LocalDateTime dataGerada = LocalDateTime.of(anoAleatorio, mesAleatorio, diaAleatorio, hora, minuto, segundo);

        if (dataGerada.isAfter(LocalDateTime.now())) {
            return LocalDateTime.now().minusDays(ThreadLocalRandom.current().nextInt(1, 30));
        }

        return dataGerada;
    }
}
