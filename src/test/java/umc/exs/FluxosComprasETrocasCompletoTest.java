package umc.exs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import umc.exs.model.compras.Troca;
import umc.exs.repository.CupomRepository;
import com.jayway.jsonpath.JsonPath;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FluxosComprasETrocasCompletoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CupomRepository cupomRepository;


    private Troca trocaParaAprovar;
    private Troca trocaParaRejeitar;

    @Test
    void fluxoCompletoTrocaAprovadaECompraComCupom() throws Exception {
        // Aprovar troca
        var respAprovar = mockMvc.perform(post("/admin/exchanges/" + trocaParaAprovar.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.troca.status").value("APPROVED"))
                .andExpect(jsonPath("$.cupom").isNotEmpty())
                .andReturn();

        String cupomCodigo = JsonPath.read(respAprovar.getResponse().getContentAsString(), "$.cupom");

        // Rejeitar outra troca
        mockMvc.perform(post("/admin/exchanges/" + trocaParaRejeitar.getId() + "/reject")
                .param("motivo", "Produto danificado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.motivoRejeicao").value("Produto danificado"));

        // Simula compra com cupom (apenas validando se o cupom existe)
        // Simula compra com cupom (apenas validando se o cupom existe)
        var cupom = cupomRepository.findByCodigo(cupomCodigo)
                .orElseThrow(() -> new RuntimeException("Cupom não encontrado"));
        assertNotNull(cupom);
        assertEquals(200.0f, cupom.getValor());

    }
}
