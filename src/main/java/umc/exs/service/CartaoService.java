package umc.exs.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import umc.exs.model.daos.mappers.CartaoMapper;
import umc.exs.model.daos.repository.CartaoRepository;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;

@Service
public class CartaoService {

    @Autowired
    private CartaoRepository cartaoRepository;

    @Transactional
    public Cartao saveOrReuseCartao(CartaoDTO dto) {
        String validadeStr = CartaoMapper.yearMonthToString(dto.getValidade());
        Optional<Cartao> cartaoReutilizado = cartaoRepository.findByValueFields(
                dto.getNumero(), dto.getNomeTitular(), validadeStr, dto.getBandeira(), dto.getCpfTitular());

        if (cartaoReutilizado.isPresent()) {
            return cartaoReutilizado.get();
        }

        Cartao cartao = CartaoMapper.toEntity(dto);
        return cartaoRepository.save(cartao);
    }

    @Transactional
    public void deletarCartaoDoCliente(Cliente cliente, Long cartaoId) {
        Cartao cartao = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado ou não pertence ao cliente."));

        cliente.getCartoes().remove(cartao);
        cartao.getClientes().remove(cliente);

        if (cartao.getClientes().isEmpty()) {
            cartaoRepository.delete(cartao);
        }
    }
}
