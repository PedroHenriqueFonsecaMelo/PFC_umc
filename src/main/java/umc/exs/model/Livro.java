package umc.exs.model;

import jakarta.persistence.*;
import umc.exs.model.foundation.Produto;

@Entity
@Table(name = "livro")
public class Livro extends Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String autor;
    private int ano;
    private String editora;
    private int edicao;
    private String isbn;
    private int npaginas;
    private String sinopse;
    private float altura;
    private float largura;
    private String categorias;
    private float peso;
    private float profundidade;
    private String barras;
    private int quant;

    // Getters e Setters
    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public int getAno() { return ano; }
    public void setAno(int ano) { this.ano = ano; }

    public String getEditora() { return editora; }
    public void setEditora(String editora) { this.editora = editora; }

    public int getEdicao() { return edicao; }
    public void setEdicao(int edicao) { this.edicao = edicao; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public int getNpaginas() { return npaginas; }
    public void setNpaginas(int npaginas) { this.npaginas = npaginas; }

    public String getSinopse() { return sinopse; }
    public void setSinopse(String sinopse) { this.sinopse = sinopse; }

    public float getAltura() { return altura; }
    public void setAltura(float altura) { this.altura = altura; }

    public float getLargura() { return largura; }
    public void setLargura(float largura) { this.largura = largura; }

    public String getCategorias() { return categorias; }
    public void setCategorias(String categorias) { this.categorias = categorias; }

    public float getPeso() { return peso; }
    public void setPeso(float peso) { this.peso = peso; }

    public float getProfundidade() { return profundidade; }
    public void setProfundidade(float profundidade) { this.profundidade = profundidade; }

    public String getBarras() { return barras; }
    public void setBarras(String barras) { this.barras = barras; }

    public int getQuant() { return quant; }
    public void setQuant(int quant) { this.quant = quant; }
}