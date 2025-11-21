package umc.exs.model.daos.mappers;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import umc.exs.model.dtos.interfaces.CartaoConvertible;
import umc.exs.model.dtos.user.CartaoDTO;
import umc.exs.model.entidades.usuario.Cartao;

public class CartaoMapper {

    // Padrão preferencial para apresentação e persistência (MM/yyyy)
    private static final DateTimeFormatter PRESENTATION_FORMATTER = DateTimeFormatter.ofPattern("MM/yyyy");
    // Aceitar também formato ISO yyyy-MM ao ler do banco, se houver discrepância histórica
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Converte a Entidade Cartao para CartaoDTO, aplicando máscara no número do cartão.
     * Faz parsing robusto da validade (suporta "MM/yyyy" e "yyyy-MM").
     */
    public static CartaoDTO fromEntity(Cartao cartao) {
        if (cartao == null) {
            return null;
        }

        CartaoDTO dto = new CartaoDTO();
        dto.setId(cartao.getId());

        // Mascaramento do número do cartão (exibe apenas últimos 4 dígitos)
        dto.setNumero(maskCardNumber(cartao.getNumero()));

        dto.setBandeira(cartao.getBandeira());
        dto.setNomeTitular(cartao.getNomeTitular());

        // CPF do titular é informação sensível — mantenha conforme política (aqui só mapeamos, considerar não expor via API)
        dto.setCpfTitular(cartao.getCpfTitular());

        // TRATAMENTO DA VALIDADE: converte String da entidade para YearMonth do DTO (se possível)
        if (cartao.getValidade() != null && !cartao.getValidade().isBlank()) {
            YearMonth ym = stringToYearMonth(cartao.getValidade());
            dto.setValidade(ym); // pode ficar null se parsing falhar
        } else {
            dto.setValidade(null);
        }

        return dto;
    }

    /**
     * Converte DTO (ou qualquer CartaoConvertible) para a Entidade Cartao.
     * Validade em YearMonth -> persistimos como "MM/yyyy".
     * Atenção: a lógica de criptografia/tokenização do número do cartão e do CVV deve ocorrer no Service.
     */
    public static Cartao toEntity(CartaoConvertible cartaoDTO) {
        if (cartaoDTO == null) {
            return null;
        }

        Cartao c = new Cartao();
        c.setId(cartaoDTO.getId());

        // O número COMPLETO é mapeado para ser persistido (Service deve criptografar/tokenizar antes de salvar)
        c.setNumero(cartaoDTO.getNumero());

        c.setBandeira(cartaoDTO.getBandeira());
        c.setNomeTitular(cartaoDTO.getNomeTitular());
        c.setCpfTitular(cartaoDTO.getCpfTitular());

        // Validade: YearMonth -> String ("MM/yyyy")
        if (cartaoDTO.getValidade() != null) {
            c.setValidade(yearMonthToString(cartaoDTO.getValidade()));
        } else {
            c.setValidade(null);
        }

        // CVV NÃO deve ser persistido
        return c;
    }

    /**
     * Converte YearMonth para String no formato de apresentação/persistência (MM/yyyy).
     * Use este método sempre que for passar validade como parâmetro para repositório/queries.
     */
    public static String yearMonthToString(YearMonth ym) {
        if (ym == null) return null;
        return ym.format(PRESENTATION_FORMATTER);
    }

    /**
     * Faz parsing robusto de String para YearMonth.
     * Suporta "MM/yyyy" e "yyyy-MM". Retorna null se não for possível parsear.
     */
    public static YearMonth stringToYearMonth(String s) {
        if (s == null || s.isBlank()) return null;
        String trimmed = s.trim();
        // Tentar primeiro o formato preferencial MM/yyyy
        try {
            return YearMonth.parse(trimmed, PRESENTATION_FORMATTER);
        } catch (DateTimeParseException e) {
            // tentar formato ISO yyyy-MM
            try {
                return YearMonth.parse(trimmed, ISO_FORMATTER);
            } catch (DateTimeParseException ex) {
                // tentar parse padrão YearMonth (ex.: "2026-07")
                try {
                    return YearMonth.parse(trimmed);
                } catch (DateTimeParseException ignored) {
                    return null;
                }
            }
        }
    }

    /**
     * Método utilitário para mascarar o número do cartão, exibindo apenas os últimos 4 dígitos.
     */
    private static String maskCardNumber(String number) {
        if (number == null || number.length() <= 4) {
            return number;
        }

        String visiblePart = number.substring(number.length() - 4);
        String maskedPart = number.substring(0, number.length() - 4).replaceAll("[0-9]", "*");
        maskedPart = maskedPart.replaceAll("(.{4})", "$1 ").trim();

        return maskedPart + " " + visiblePart;
    }
}