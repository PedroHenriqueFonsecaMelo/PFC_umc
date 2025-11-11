package umc.exs.model.daos.mappers;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import umc.exs.model.dtos.interfaces.CartaoConvertible;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

public class CartaoMapper {
    
    // Padrão de formato para validade (assumindo MM/yyyy)
    private static final DateTimeFormatter VALIDATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");

    /**
     * Converte a Entidade Cartao para CartaoDTO, aplicando a máscara no número do cartão.
     */
    public static CartaoDTO fromEntity(Cartao cartao) {
        if (cartao == null) {
            return null;
        }

        CartaoDTO dto = new CartaoDTO();
        dto.setId(cartao.getId());
        
        // 1. PSEUDO-ANONIMIZAÇÃO (Mascaramento)
        dto.setNumero(maskCardNumber(cartao.getNumero())); 
        
        dto.setBandeira(cartao.getBandeira());
        dto.setNomeTitular(cartao.getNomeTitular());
        dto.setCpfTitular(cartao.getCpfTitular());
        
        // 2. TRATAMENTO DA VALIDADE (String da Entidade para YearMonth do DTO)
        if (cartao.getValidade() != null && !cartao.getValidade().isBlank()) {
            dto.setValidade(YearMonth.parse(cartao.getValidade(), VALIDATE_FORMATTER)); 
        }

        // O CVV no DTO será nulo (não é recuperado do DB)
        // O DTO existe para ser usado na camada de apresentação (Front-end)
        
        return dto;
    }

    /**
     * Converte DTO (ou qualquer CartaoConvertible) para a Entidade Cartao.
     */
    public static Cartao toEntity(CartaoConvertible cartaoDTO) {
        if (cartaoDTO == null) {
            return null;
        }
        
        Cartao c = new Cartao();
        c.setId(cartaoDTO.getId());
        
        // O número COMPLETO é mapeado para ser persistido.
        // A lógica de criptografia/tokenização deve ser aplicada pelo Service antes de salvar.
        c.setNumero(cartaoDTO.getNumero()); 
        
        c.setBandeira(cartaoDTO.getBandeira());
        c.setNomeTitular(cartaoDTO.getNomeTitular());
        c.setCpfTitular(cartaoDTO.getCpfTitular());
        
        // TRATAMENTO DA VALIDADE (YearMonth do DTO para String da Entidade)
        if (cartaoDTO.getValidade() != null) {
             c.setValidade(cartaoDTO.getValidade().format(VALIDATE_FORMATTER));
        }

        // CVV NÃO DEVE SER PERSISTIDO na Entidade
        
        return c;
    }
    
    /**
     * Método utilitário para mascarar o número do cartão, exibindo apenas os últimos 4 dígitos.
     */
    private static String maskCardNumber(String number) {
        if (number == null || number.length() <= 4) {
            return number;
        }
        
        String visiblePart = number.substring(number.length() - 4);
        
        // Cria uma string de asteriscos para a parte mascarada e formata com espaços
        String maskedPart = number.substring(0, number.length() - 4).replaceAll("[0-9]", "*");
        maskedPart = maskedPart.replaceAll("(.{4})", "$1 ").trim();
        
        return maskedPart + " " + visiblePart; 
    }
}