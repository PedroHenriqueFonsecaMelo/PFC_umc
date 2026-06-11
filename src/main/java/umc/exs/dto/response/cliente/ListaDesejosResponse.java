package umc.exs.dto.response.cliente;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO de resposta de um item da lista de desejos do cliente.
 * Contém ISBN do livro desejado, data de adição e status de pré-reserva.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListaDesejosResponse {

    // Identificador do item na lista de desejos
    private Long id;

    // ISBN do livro desejado pelo cliente
    private String isbn;

    // Data e hora em que o livro foi adicionado à lista de desejos
    private LocalDateTime dataAdicao;

    // Indica se o cliente quer ser priorizado na notificação quando o livro ficar disponível
    private boolean preReservaAtiva;
}
