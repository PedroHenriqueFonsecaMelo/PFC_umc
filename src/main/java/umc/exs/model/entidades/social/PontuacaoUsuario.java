package umc.exs.model.entidades.social;

import jakarta.persistence.*;
import lombok.*;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.enums.NivelUsuario;

import java.time.LocalDateTime;

/**
 * Armazena o XP acumulado de cada cliente para o sistema de gamificação.
 * Relacionamento 1-para-1 com Cliente.
 */
@Entity
@Table(name = "pontuacao_usuario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PontuacaoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @Builder.Default
    @Column(nullable = false)
    private Integer xpTotal = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer xpLivrosAprovados = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer xpCompras = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer xpAvaliacoes = 0;

    @Column(nullable = false)
    private LocalDateTime ultimaAtualizacao;

    /**
     * Calcula o nível/badge com base no XP total atual.
     */
    @Transient
    public NivelUsuario getNivel() {
        return NivelUsuario.calcular(this.xpTotal == null ? 0 : this.xpTotal);
    }

    /**
     * Adiciona XP e atualiza a categoria correspondente.
     */
    public void adicionarXp(int quantidade, String categoria) {
        if (this.xpTotal == null)
            this.xpTotal = 0;
        this.xpTotal += quantidade;
        this.ultimaAtualizacao = LocalDateTime.now();

        switch (categoria) {
            case "APROVACAO" -> {
                if (this.xpLivrosAprovados == null)
                    this.xpLivrosAprovados = 0;
                this.xpLivrosAprovados += quantidade;
            }
            case "COMPRA" -> {
                if (this.xpCompras == null)
                    this.xpCompras = 0;
                this.xpCompras += quantidade;
            }
            case "AVALIACAO" -> {
                if (this.xpAvaliacoes == null)
                    this.xpAvaliacoes = 0;
                this.xpAvaliacoes += quantidade;
            }
        }
    }
}
