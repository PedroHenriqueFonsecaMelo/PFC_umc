package umc.exs.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envelope genérico de resposta da API REST, padronizando sucesso, mensagem, dados e erro em todas as respostas do sistema.
 * Utilizado pelos controllers para garantir um formato consistente independente do tipo de retorno.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExternApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    /**
     * Cria uma resposta de sucesso com dados e mensagem descritiva.
     * Utilizado quando a operação retorna um objeto como resultado.
     */
    public static <T> ExternApiResponse<T> ok(T data, String message) {
        return ExternApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Cria uma resposta de sucesso sem dados, apenas com mensagem.
     * Utilizado em operações void onde não há objeto a retornar.
     */
    public static ExternApiResponse<Void> ok(String message) {
        return ExternApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Cria uma resposta de falha com mensagem de erro.
     * Define success como false e preenche os campos message e error com o motivo da falha.
     */
    public static <T> ExternApiResponse<T> fail(String message) {
        return ExternApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(message)
                .build();
    }
}
