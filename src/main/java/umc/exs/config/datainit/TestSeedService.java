package umc.exs.config.datainit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.foundation.Lote;
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
import umc.exs.repository.negocios.RespostaForumRepository;
import umc.exs.repository.negocios.TopicoForumRepository;
import umc.exs.repository.usuario.ClienteRepository;
import umc.exs.repository.usuario.PontuacaoUsuarioRepository;
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
    private final TopicoForumRepository topicoRepo;
    private final RespostaForumRepository respostaRepo;
    private final CupomRepository cupomRepo;
    private final PasswordEncoder encoder;
    private final PontosSchedulerService pontosSchedulerService;

    public void run() {

        if (adminRepo.count() > 0)
            return;

        // ADMIN
        Administrador admin = new Administrador();
        admin.setNome("Admin Master");
        admin.setEmail("admin@admin.com");
        admin.setPassword(encoder.encode("admin123"));
        admin = adminRepo.save(admin);

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
        // Evitar cliente nulo para reduzir risco de violações de constraint em schemas
        // diferentes.
        saveCupom("BEMVINDO10", c1);
        saveCupom("PRESENTE50", c1);
        saveCupom("XP-LOYALTY", c2);

        // FORUM
        TopicoForum t = new TopicoForum();
        t.setTitulo("Machado de Assis");
        t.setConteudo("Qual obra começar?");
        t.setAutor(c1);
        t.setCategoria(CategoriaForum.GERAL);
        t.setDataCriacao(LocalDateTime.now());
        topicoRepo.save(t);

        RespostaForum r = new RespostaForum();
        r.setConteudo("Dom Casmurro!");
        r.setAutor(c2);
        r.setTopico(t);
        respostaRepo.save(r);

        // LIVROS
        Lote lote = new Lote();
        lote.setCliente(c1);
        lote.setCodigoProtocolo("PROT-TESTE");
        lote.setStatus(Lote.LoteStatus.PENDENTE);
        loteRepo.save(lote);

        // Salvando livros em análise e aprovados
        saveLivro("1984", "George Orwell", "9780060918111", lote, false, null, null);
        saveLivro("Dom Casmurro", "Machado de Assis", "9788535902778", null, true, 30.0, admin.getId());
        saveLivro("O Hobbit", "J.R.R. Tolkien", "9780547928227", null, true, 45.0, admin.getId());

        pontosSchedulerService.processarDecayXp();

        log.info(" TEST SEED finalizado");
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
        c.setDataCriacao(LocalDateTime.now());
        return clienteRepo.save(c);
    }

    private void saveLivro(String tit, String aut, String isbn, Lote lote,
            boolean aprov, Double preco, Long adminId) {
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
        cupom.setDataCriacao(LocalDateTime.now());
        cupomRepo.save(cupom);
    }
}
