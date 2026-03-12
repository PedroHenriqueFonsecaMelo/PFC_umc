package umc.exs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.model.dtos.AdminAprovacaoDTO;
import umc.exs.model.dtos.LivroRequestDTO;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.LivroRepository;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;
import umc.exs.model.entidades.usuario.Cliente;

@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    // Quantidade de tokens ganhos ao cadastrar um livro
    private static final double TOKEN_REWARD = 10.0;

    @Transactional
    public LivroAnuncio cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
        Cliente vendedor;
        Optional<Cliente> vendedorOpt = clienteRepository.findByEmail(email);
        if (vendedorOpt.isEmpty()) {
            throw new RuntimeException("Vendedor não encontrado");
        }
        vendedor = vendedorOpt.get();

        // Simulação de Upload (Salvando localmente na pasta uploads)
        String nomeFoto = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path caminho = Paths.get("uploads/livros/" + nomeFoto);
        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto");
        }

        // Criar o anúncio com status "aprovado = false" (pendente)
        // O usuário NÃO define mais o preço - o admin definirá
        LivroAnuncio anuncio = LivroAnuncio.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .isbn(dto.getIsbn())
                // preço e estado serão definidos pelo admin durante a aprovação
                .fotoUrl("/uploads/livros/" + nomeFoto)
                .vendedor(vendedor)
                .dataAnuncio(LocalDateTime.now())
                .aprovado(false)
                .build();

        LivroAnuncio salvo = livroRepository.save(anuncio);

        // Give token reward to the user for registering a book
        vendedor.setSaldoTokens(vendedor.getSaldoTokens() + TOKEN_REWARD);
        clienteRepository.save(vendedor);

        // Log da recompensa
        logAuditoria.registrarLog("LIVRO_CADASTRADO_RECOMPENSA", vendedor.getId(), vendedor.getEmail(),
                "Cadastrou livro ID " + salvo.getId() + " e recebeu T$ " + TOKEN_REWARD + " de recompensa");

        return salvo;
    }

    // Listar apenas livros APROVADOS (para a vitrine)
    public List<LivroAnuncio> listarLivrosAprovados() {
        return livroRepository.findByAprovadoTrue();
    }

    // Listar livros pendentes (para o admin)
    public List<LivroAnuncio> listarLivrosPendentes() {
        return livroRepository.findByAprovadoFalse();
    }

    // Listar todos os livros (para admin)
    public List<LivroAnuncio> listarTodosLivros() {
        return livroRepository.findAll();
    }

    // Aprovar livro (admin define preço, estado e comentário)
    @Transactional
    public LivroAnuncio aprovarLivro(Long livroId, Long adminId, AdminAprovacaoDTO dto) {
        LivroAnuncio anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        // Converte e busca o preço direto do Enum (Segurança total)
        EstadoLivro estado = EstadoLivro.valueOf(dto.getEstadoAprovado().toString().toUpperCase());

        anuncio.setAprovado(true);
        anuncio.setEstadoAprovado(estado);
        anuncio.setPrecoAprovado((double) estado.getPreco()); // O preço vem do Enum!
        anuncio.setAdminAprovadorId(adminId);
        anuncio.setDataAprovacao(LocalDateTime.now());

        return livroRepository.save(anuncio);
    }

    // Rejeitar livro (admin envia comentário)
    @Transactional
    public void rejeitarLivro(Long livroId, Long adminId, String comentario) {
        LivroAnuncio anuncio = livroRepository.findById(livroId)
                .orElseThrow(() -> new RuntimeException("Livro não encontrado"));

        // registrar log antes de deletar
        logAuditoria.registrarLog("LIVRO_REJEITADO", adminId, null,
                "Livro ID " + livroId + " rejeitado. Comentário: " + comentario);

        livroRepository.delete(anuncio);
    }

    @Transactional
    public void realizarCompra(Long livroId, String emailComprador) {
        // Buscar apenas livros APROVADOS
        Optional<LivroAnuncio> anuncioOpt = livroRepository.findByIdAndAprovadoTrue(livroId);
        if (anuncioOpt.isEmpty()) {
            throw new RuntimeException("Anúncio não encontrado ou não aprovado.");
        }
        LivroAnuncio anuncio = anuncioOpt.get();

        Optional<Cliente> compradorOpt = clienteRepository.findByEmail(emailComprador);
        if (compradorOpt.isEmpty()) {
            throw new RuntimeException("Comprador não encontrado.");
        }
        Cliente comprador = compradorOpt.get();

        Cliente vendedor = anuncio.getVendedor();

        if (vendedor.getId().equals(comprador.getId())) {
            throw new RuntimeException("Você não pode comprar seu próprio anúncio.");
        }

        // o anúncio só existe se aprovado; preço final é sempre o definido pelo admin
        Double precoFinal = anuncio.getPrecoAprovado();
        if (precoFinal == null) {
            throw new RuntimeException("Preço aprovado não definido, não é possível comprar.");
        }

        if (comprador.getSaldoTokens() < precoFinal) {
            throw new RuntimeException("Saldo insuficiente! Você precisa de T$ " + precoFinal);
        }

        // Transferência de Tokens
        comprador.setSaldoTokens(comprador.getSaldoTokens() - precoFinal);
        vendedor.setSaldoTokens(vendedor.getSaldoTokens() + precoFinal);

        // Finalizar Anúncio (Remover da vitrine)
        livroRepository.delete(anuncio);

        // Persistir mudanças
        clienteRepository.save(comprador);
        clienteRepository.save(vendedor);

        // Auditoria
        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(),
                "Comprou livro ID " + livroId + " por T$ " + precoFinal);
    }
}
