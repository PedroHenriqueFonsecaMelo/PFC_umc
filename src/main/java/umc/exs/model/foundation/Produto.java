package umc.exs.model.foundation;

import jakarta.persistence.*;

@MappedSuperclass
public abstract class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private float precificacao;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public float getPrecificacao() { return precificacao; }
    public void setPrecificacao(float precificacao) { this.precificacao = precificacao; }
}