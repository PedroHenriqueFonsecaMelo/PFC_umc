package umc.exs.handler;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exceção lançada quando o cliente excede o limite de requisições definido pelo RateLimitFilter.
 * Mapeada automaticamente para HTTP 429 Too Many Requests pelo Spring via @ResponseStatus.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {

    /**
     * Cria a exceção com a mensagem padrão de limite excedido.
     * Utilizado quando não há necessidade de personalizar a mensagem de erro.
     */
    public TooManyRequestsException() {
        super("Muitas requisições. Tente novamente mais tarde.");
    }

    /**
     * Cria a exceção com uma mensagem personalizada de erro.
     * Utilizado quando o contexto exige uma mensagem mais específica para o cliente.
     */
    public TooManyRequestsException(String message) {
        super(message);
    }
}
