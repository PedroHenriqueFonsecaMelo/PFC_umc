package umc.exs.Teste;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import umc.exs.model.foundation.Produto;
import umc.exs.repository.ProdutoRepository;

@RestController
@RequestMapping("/test/produto")
public class ControllerProdutoTeste {
    @Autowired
    private ProdutoRepository produtoRepository;

    @PostMapping("/cadastrar-produto")
    public ResponseEntity<Produto> cadastrarProduto() {
        Produto produto = new Produto();
        produto.setTitulo("Fone de Ouvido Bluetooth");
        produto.setPrecificacao(199.90f);

        Produto saved = produtoRepository.save(produto);
        return ResponseEntity.ok(saved);
    }
}
