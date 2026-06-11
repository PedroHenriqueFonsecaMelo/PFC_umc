package umc.exs.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção de negócio da aplicação, lançada quando uma regra de negócio é violada.
 * Mapeada automaticamente para HTTP 400 Bad Request pelo Spring via @ResponseStatus.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

    /**
     * Cria a exceção com a mensagem descrevendo a regra de negócio violada.
     * A mensagem é retornada ao cliente na resposta de erro.
     */
    public BusinessException(String message) {
        super(message);
    }
}
