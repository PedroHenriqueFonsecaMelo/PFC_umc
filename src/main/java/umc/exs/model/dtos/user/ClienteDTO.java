package umc.exs.model.dtos.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import umc.exs.model.dtos.interfaces.ClienteConvertible;
import umc.exs.model.entidades.usuario.Cliente;

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

    private List<EnderecoDTO> enderecos;
    private List<CartaoDTO> cartoes;

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
                    .collect(java.util.stream.Collectors.toSet()));
        }
        if (this.cartoes != null && !this.cartoes.isEmpty()) {
            c.setCartoes(this.cartoes.stream()
                    .map(CartaoDTO::toEntity)
                    .collect(java.util.stream.Collectors.toSet()));
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

        if (c.getEnderecos() != null && !c.getEnderecos().isEmpty()) {
            dto.enderecos = c.getEnderecos().stream()
                    .map(EnderecoDTO::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
        }
        if (c.getCartoes() != null && !c.getCartoes().isEmpty()) {
            dto.cartoes = c.getCartoes().stream()
                    .map(CartaoDTO::fromEntity)
                    .collect(java.util.stream.Collectors.toList());
        }
        return dto;
    }

    public void addEndereco(EnderecoDTO enderecoDTO) {
        this.enderecos.add(enderecoDTO);
    }

    public void addCartao(CartaoDTO cartaoDTO) {
        this.cartoes.add(cartaoDTO);
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
