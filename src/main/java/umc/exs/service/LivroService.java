package umc.exs.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.log.LogAuditoriaService;
import umc.exs.model.daos.repository.ClienteRepository;
import umc.exs.model.daos.repository.LivroRepository;
import umc.exs.model.dtos.LivroRequestDTO;
import umc.exs.model.entidades.foundation.LivroAnuncio;
import umc.exs.model.entidades.foundation.enums.EstadoLivro;
import umc.exs.model.entidades.usuario.Cliente;

@Service
@RequiredArgsConstructor
public class LivroService {

    private final LivroRepository livroRepository, repository;
    private final ClienteRepository clienteRepository;
    private final LogAuditoriaService logAuditoria;

    @Transactional
    public LivroAnuncio cadastrarVenda(String email, LivroRequestDTO dto, MultipartFile foto) {
        Cliente vendedor = clienteRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Vendedor não encontrado"));

        // Simulação de Upload (Salvando localmente na pasta uploads)
        String nomeFoto = UUID.randomUUID() + "_" + foto.getOriginalFilename();
        Path caminho = Paths.get("uploads/livros/" + nomeFoto);
        try {
            Files.createDirectories(caminho.getParent());
            Files.copy(foto.getInputStream(), caminho);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar foto");
        }

        // Lógica de "Simulação de Avaliação"
        // Aqui poderíamos ter uma IA, mas vamos validar se o preço condiz com o estado
        validarPrecoPorEstado(dto.getPrecoTokens(), dto.getEstado());

        LivroAnuncio anuncio = LivroAnuncio.builder()
                .titulo(dto.getTitulo())
                .autor(dto.getAutor())
                .precoTokens(dto.getPrecoTokens())
                .estado(dto.getEstado())
                .fotoUrl("/uploads/livros/" + nomeFoto)
                .vendedor(vendedor)
                .dataAnuncio(LocalDateTime.now())
                .build();

        return repository.save(anuncio);
    }

    private void validarPrecoPorEstado(Double preco, EstadoLivro estado) {
        // Exemplo: Um livro DESGASTADO não pode custar mais de 50 tokens
        if (estado == EstadoLivro.DESGASTADO && preco > 50.0) {
            throw new IllegalArgumentException("Preço muito alto para um livro neste estado.");
        }
    }

    @Transactional
    public void realizarCompra(Long livroId, String emailComprador) {
        // 1. Validar existência do anúncio e das partes
        LivroAnuncio anuncio = livroRepository.findById(livroId)
            .orElseThrow(() -> new RuntimeException("Anúncio não encontrado."));
        
        Cliente comprador = clienteRepository.findByEmail(emailComprador)
            .orElseThrow(() -> new RuntimeException("Comprador não encontrado."));
        
        Cliente vendedor = anuncio.getVendedor();

        // 2. Impedir que o usuário compre o próprio livro
        if (vendedor.getId().equals(comprador.getId())) {
            throw new RuntimeException("Você não pode comprar seu próprio anúncio.");
        }

        // 3. Validar Saldo
        if (comprador.getSaldoTokens() < anuncio.getPrecoTokens()) {
            throw new RuntimeException("Saldo insuficiente! Você precisa de T$ " + anuncio.getPrecoTokens());
        }

        // 4. Transferência de Tokens
        comprador.setSaldoTokens(comprador.getSaldoTokens() - anuncio.getPrecoTokens());
        vendedor.setSaldoTokens(vendedor.getSaldoTokens() + anuncio.getPrecoTokens());

        // 5. Finalizar Anúncio (Remover da vitrine)
        livroRepository.delete(anuncio);

        // 6. Persistir mudanças
        clienteRepository.save(comprador);
        clienteRepository.save(vendedor);

        // 7. Auditoria
        logAuditoria.registrarLog("COMPRA_LIVRO_SUCESSO", comprador.getId(), comprador.getEmail(), 
            "Comprou livro ID " + livroId + " por T$ " + anuncio.getPrecoTokens());
    }
}
