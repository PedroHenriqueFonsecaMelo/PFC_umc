package umc.exs.service.cupom;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import umc.exs.dto.request.admin.CriarCupomRequest;
import umc.exs.model.entidades.foundation.Cupom;
import umc.exs.model.entidades.usuario.Cliente;
import umc.exs.repository.negocios.CupomRepository;
import umc.exs.repository.negocios.CupomUsoRepository;
import umc.exs.repository.usuario.ClienteRepository;

@ExtendWith(MockitoExtension.class)
class CupomServiceTest {

    @Mock
    CupomRepository cupomRepository;

    @Mock
    CupomUsoRepository cupomUsoRepository;

    @Mock
    ClienteRepository clienteRepository;

    @InjectMocks
    CupomService service;

    @Test
    void gerarCupomPorPontuacao_quandoClienteNaoEncontrado_lanca() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.gerarCupomPorPontuacao(1L));
    }

    @Test
    void criarCupom_quandoCodigoNulo_geraCupom() {
        CriarCupomRequest dto = new CriarCupomRequest();
        dto.setCodigo(null);
        dto.setPercentualDesconto(20.0);
        dto.setQuantidadeMaxima(2);

        when(cupomRepository.existsByCodigo(anyString())).thenReturn(false);
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(i -> i.getArgument(0));

        Cupom cupom = service.criarCupom(dto, LocalDateTime.now().plusDays(10));

        assertNotNull(cupom.getCodigo());
        assertEquals(20.0, cupom.getPercentualDesconto());
        assertEquals(2, cupom.getQuantidadeMaxima());
    }

    @Test
    void validarCupomParaTotal_quandoCupomValido_retornaResumo() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setEmail("u@test.com");

        Cupom cupom = Cupom.builder()
                .id(1L)
                .codigo("CUPOM")
                .percentualDesconto(10.0)
                .expiracao(LocalDateTime.now().plusDays(1))
                .usado(false)
                .build();

        when(clienteRepository.findByEmail("u@test.com")).thenReturn(Optional.of(cliente));
        when(cupomRepository.findByCodigo("CUPOM")).thenReturn(Optional.of(cupom));
        when(cupomUsoRepository.existsByCupomIdAndClienteId(1L, 1L)).thenReturn(false);

        var resultado = service.validarCupomParaTotal("CUPOM", "u@test.com", 100.0);

        assertTrue((Boolean) resultado.get("valido"));
        assertEquals(10.0, (Double) resultado.get("desconto"));
    }

    @Test
    void aplicarCupomCarrinho_quandoCupomValido_aplicaDesconto() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Cupom cupom = Cupom.builder()
                .id(1L)
                .codigo("CUPOM")
                .percentualDesconto(20.0)
                .expiracao(LocalDateTime.now().plusDays(1))
                .usado(false)
                .quantidadeUsada(0)
                .build();

        when(cupomRepository.findByCodigo("CUPOM")).thenReturn(Optional.of(cupom));
        when(cupomUsoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cupomRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        double total = service.aplicarCupomCarrinho("CUPOM", cliente, 100.0);

        assertEquals(80.0, total);
        verify(cupomUsoRepository).save(any());
        verify(cupomRepository).save(cupom);
    }
}
