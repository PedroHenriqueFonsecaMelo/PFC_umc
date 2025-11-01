package umc.exs.model.foundation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import java.math.BigDecimal;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private BigDecimal precificacao;  // Usando BigDecimal para maior precisão

    @Column(nullable = false, length = 2000)
    private String descricaoDoProduto;  // Nome em camelCase

    // Getters e setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public BigDecimal getPrecificacao() {
        return precificacao;
    }

    public void setPrecificacao(BigDecimal precificacao) {
        this.precificacao = precificacao;
    }

    public String getDescricaoDoProduto() {
        return descricaoDoProduto;
    }

    public void setDescricaoDoProduto(String descricaoDoProduto) {
        this.descricaoDoProduto = descricaoDoProduto;
    }

    public void setPrecificacao(float f) {
        this.precificacao = BigDecimal.valueOf(f);
    }
}
