package umc.exs.service.core;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.mappers.CartaoMapper;
import umc.exs.mappers.DateMapper;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.CartaoRepository;

@Service
@RequiredArgsConstructor
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final CartaoMapper cartaoMapper;
    private final DateMapper dateMapper; 

    @Transactional
    public Cartao saveOrReuseCartao(CartaoDTO dto) {
        String validadeStr = dateMapper.yearMonthToString(dto.getValidade());

        Optional<Cartao> cartaoReutilizado = cartaoRepository.findByValueFields(
                dto.getNumero(),
                dto.getNomeTitular(),
                validadeStr,
                dto.getBandeira(),
                dto.getCpfTitular());

        if (cartaoReutilizado.isPresent()) {
            return cartaoReutilizado.get();
        }

        Cartao cartao = cartaoMapper.paraEntidade(dto);
        return cartaoRepository.save(cartao);
    }

    @Transactional
    public void deletarCartaoDoCliente(Cliente cliente, Long cartaoId) {
        Cartao cartaoParaRemover = cliente.getCartoes().stream()
                .filter(c -> c.getId().equals(cartaoId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado ou não pertence ao cliente."));

        cliente.getCartoes().remove(cartaoParaRemover);
        cartaoParaRemover.getClientes().remove(cliente);

        if (cartaoParaRemover.getClientes().isEmpty()) {
            cartaoRepository.delete(cartaoParaRemover);
        }
    }
}