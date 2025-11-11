package umc.exs.model.entidades.usuario;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cartao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String numero;

    @Column
    private String bandeira;

    @Column(nullable = false)
    private String nomeTitular;

    @Column
    private String validade; 
    // REMOVIDO: CVV NÃO DEVE SER PERSISTIDO por razões de segurança (PCI DSS).
    // private String cvv; 

    @Column(nullable = false)
    private String cpfTitular; // NOVO: Campo de CPF do titular adicionado

    @ManyToMany(mappedBy = "cartoes")
    private Set<Cliente> clientes = new HashSet<>();

    // --- Métodos de Relacionamento e Utility ---

    // Getters e setters para clientes (garantidos pelo Lombok, mas mantidos para b-directional helper se necessário)
    public Set<Cliente> getClientes() {
        return clientes;
    }

    public void setClientes(Set<Cliente> clientes) {
        this.clientes = clientes;
    }

    // Método helper para adicionar a relação bi-direcional
    public void addCliente(Cliente cliente) {
        this.clientes.add(cliente);
        cliente.getCartoes().add(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Cartao other = (Cartao) obj;
        
        // Compara por ID, que é a chave primária
        return id != null && Objects.equals(id, other.id);
    }

}