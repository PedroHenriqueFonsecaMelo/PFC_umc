package umc.exs.service.core.cliente;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import umc.exs.DTOs.user.CartaoDTO;
import umc.exs.mappers.CartaoMapper;
import umc.exs.mappers.DateMapper;
import umc.exs.model.entidades.usuario.Cartao;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.usuario.CartaoRepository;

@Service
@RequiredArgsConstructor
public class CartaoService {

    private final CartaoRepository cartaoRepository;
    private final CartaoMapper cartaoMapper;
    private final DateMapper dateMapper;

    @SuppressWarnings("null")
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

    @Transactional
    public void vincularNovoCartao(Cliente cliente, CartaoDTO dto) {
        Cartao cartao = saveOrReuseCartao(dto);
        if (!cliente.getCartoes().contains(cartao)) {
            cliente.getCartoes().add(cartao);
            cartao.getClientes().add(cliente);
        }
    }

    @Transactional
    public void sincronizarCartoes(Cliente cliente, List<CartaoDTO> dtos) {
        if (dtos == null) return;

        Set<Long> idsManter = dtos.stream()
                .map(CartaoDTO::getId)
                .filter(id -> id != null && id != 0)
                .collect(Collectors.toSet());

        // Remove associações antigas
        cliente.getCartoes().removeIf(cartao -> {
            if (!idsManter.contains(cartao.getId())) {
                cartao.getClientes().remove(cliente);
                return true;
            }
            return false;
        });

        // Adiciona novos
        for (CartaoDTO dto : dtos) {
            if (dto.getId() == null || dto.getId() == 0) {
                vincularNovoCartao(cliente, dto);
            }
        }
    }

}