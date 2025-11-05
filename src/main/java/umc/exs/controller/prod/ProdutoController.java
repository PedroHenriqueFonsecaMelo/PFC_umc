package umc.exs.controller.prod;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import umc.exs.model.daos.repository.ProdutoRepository;
import umc.exs.model.dtos.admin.ProdutoDTO;
import umc.exs.model.entidades.foundation.Produto;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
public class ProdutoController {

    @Autowired
    private ProdutoRepository produtoRepository;

    // Criação de um produto usando o DTO
    @PostMapping
    public ResponseEntity<ProdutoDTO> createProduto(@RequestBody ProdutoDTO produtoDTO) {
        Produto produto = produtoDTO.toEntity();
        Produto savedProduto = produtoRepository.save(produto);
        ProdutoDTO savedProdutoDTO = ProdutoDTO.fromEntity(savedProduto);
        return ResponseEntity.ok(savedProdutoDTO);
    }

    // Listar todos os produtos
    @GetMapping
    public ResponseEntity<List<ProdutoDTO>> getAllProdutos() {
        List<ProdutoDTO> produtoDTOs = produtoRepository.findAll().stream()
            .map(ProdutoDTO::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(produtoDTOs);
    }

    // Buscar um produto específico
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> getProdutoById(@PathVariable Long id) {
        return produtoRepository.findById(id)
            .map(produto -> ResponseEntity.ok(ProdutoDTO.fromEntity(produto)))
            .orElse(ResponseEntity.notFound().build());
    }

    // Atualizar um produto
    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> updateProduto(@PathVariable Long id, @RequestBody ProdutoDTO produtoDTO) {
        return produtoRepository.findById(id)
            .map(existingProduto -> {
                existingProduto.setTitulo(produtoDTO.getTitulo());
                existingProduto.setPrecificacao(produtoDTO.getPrecificacao());
                existingProduto.setDescricaoDoProduto(produtoDTO.getDescricaoDoProduto());
                Produto updatedProduto = produtoRepository.save(existingProduto);
                return ResponseEntity.ok(ProdutoDTO.fromEntity(updatedProduto));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    // Deletar um produto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduto(@PathVariable Long id) {
        if (!produtoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        produtoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
