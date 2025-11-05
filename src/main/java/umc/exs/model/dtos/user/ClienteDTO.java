package umc.exs.model.dtos.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.model.entidades.usuario.Endereco;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClienteDTO implements ClienteConvertible {
    private Long id;
    private String nome;
    private String email;
    private String datanasc;
    private String gen;
    private String senha;
    private String cpf;

    private List<EnderecoDTO> enderecos = new ArrayList<>();
    private List<CartaoDTO> cartoes = new ArrayList<>();

    // getters e setters

    @Override
    public Cliente toEntity() {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome(nome);
        c.setEmail(email);
        c.setDatanasc(datanasc);
        c.setGen(gen);
        c.setSenha(senha);
        c.setCpf(cpf);

        if (this.enderecos != null && !this.enderecos.isEmpty()) {
            c.setEnderecos(this.enderecos.stream()
                    .map(EnderecoDTO::toEntity)
                    .collect(Collectors.toSet()));
        }
        if (this.cartoes != null && !this.cartoes.isEmpty()) {
            c.setCartoes(this.cartoes.stream()
                    .map(CartaoDTO::toEntity)
                    .collect(Collectors.toSet()));
        }

        return c;
    }

    public static ClienteDTO fromEntity(Cliente c) {
        if (c == null)
            return null;
        ClienteDTO dto = new ClienteDTO();
        dto.id = c.getId();
        dto.nome = c.getNome();
        dto.email = c.getEmail();
        dto.datanasc = c.getDatanasc();
        dto.gen = c.getGen();
        dto.cpf = c.getCpf();
        dto.senha = c.getSenha();

        // Mapeia de Set (Entity) para List (DTO)
        if (c.getEnderecos() != null && !c.getEnderecos().isEmpty()) {
            dto.enderecos = c.getEnderecos().stream()
                    .map(EnderecoDTO::fromEntity)
                    .collect(Collectors.toList()); // Coleta como LIST para o Formulário/Homepage
        }
        if (c.getCartoes() != null && !c.getCartoes().isEmpty()) {
            dto.cartoes = c.getCartoes().stream()
                    .map(CartaoDTO::fromEntity)
                    .collect(Collectors.toList()); // Coleta como LIST
        }
        return dto;
    }

    public void addEndereco(EnderecoDTO enderecoDTO) {
        if (this.enderecos == null) {
            this.enderecos = new ArrayList<>();
        }
        this.enderecos.add(enderecoDTO);
    }

    public void addCartao(CartaoDTO cartaoDTO) {
        if (this.cartoes == null) {
            this.cartoes = new ArrayList<>();
        }
        this.cartoes.add(cartaoDTO);
    }

    public void mergeEnderecos(Cliente clienteExistente, List<EnderecoDTO> novosEnderecos) {
        if (novosEnderecos == null)
            return;

        for (EnderecoDTO dto : novosEnderecos) {
            Endereco novo = dto.toEntity();
            boolean existe = clienteExistente.getEnderecos().stream()
                    .anyMatch(e -> e.getId() != null && e.getId().equals(novo.getId()));

            if (!existe) {
                clienteExistente.getEnderecos().add(novo);
            }
        }
    }

    public void mergeCartoes(Cliente clienteExistente, List<CartaoDTO> novosCartoes) {
        if (novosCartoes == null)
            return;
        for (CartaoDTO dto : novosCartoes) {
            umc.exs.model.entidades.usuario.Cartao novo = dto.toEntity();
            boolean existe = clienteExistente.getCartoes().stream()
                    .anyMatch(c -> c.getId() != null && c.getId().equals(novo.getId()));

            if (!existe) {
                clienteExistente.getCartoes().add(novo);
            }
        }
    }

    @Override
    public String toString() {
        return "ClienteDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", datanasc='" + datanasc + '\'' +
                ", gen='" + gen + '\'' +
                ", senha='" + senha + '\'' +
                ", cpf='" + cpf + '\'' +
                ", enderecos=" + enderecos +
                ", cartoes=" + cartoes +
                '}';
    }
}
