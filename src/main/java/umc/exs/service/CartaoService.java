package umc.exs.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;

@Service
@RequiredArgsConstructor
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final CartaoMapper cartaoMapper; // Injetado

    @Transactional
    public Cartao saveOrReuseCartao(CartaoDTO dto) {
        // CORREÇÃO: Usando o nome do método definido no Mapper
        String validadeStr = cartaoMapper.toValidadeString(dto.getValidade());

        Optional<Cartao> cartaoReutilizado = cartaoRepository.findByValueFields(
                dto.getNumero(),
                dto.getNomeTitular(),
                validadeStr,
                dto.getBandeira(),
                dto.getCpfTitular());

        if (cartaoReutilizado.isPresent()) {
            return cartaoReutilizado.get();
        }

        Cartao cartao = cartaoMapper.toEntity(dto);
        return cartaoRepository.save(cartao);
    }

    @Transactional
    public void deletarCartaoDoCliente(Cliente cliente, Long cartaoId) {
        Cartao cartaoParaRemover = null;

        for (Cartao c : cliente.getCartoes()) {
            if (c.getId().equals(cartaoId)) {
                cartaoParaRemover = c;
                break;
            }
        }

        if (cartaoParaRemover == null) {
            throw new IllegalArgumentException("Cartão não encontrado ou não pertence ao cliente.");
        }

        cliente.getCartoes().remove(cartaoParaRemover);
        cartaoParaRemover.getClientes().remove(cliente);

        if (cartaoParaRemover.getClientes().isEmpty()) {
            cartaoRepository.delete(cartaoParaRemover);
        }
    }
}