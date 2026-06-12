package umc.exs.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Filtro de rate limiting por IP baseado no algoritmo token bucket.
 * Permite até 500 requisições por minuto por endereço IP, retornando
 * HTTP 429 com corpo JSON para APIs ou erro padrão para requisições web
 * quando o limite é excedido.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int CAPACITY = 500;
    private static final long REFILL_INTERVAL_MS = 60_000;

    private static final String[] SKIP_PATHS = {
            "/css/", "/js/", "/images/", "/favicon.ico", "/error",
            "/actuator/", "/h2-console/", "/swagger-ui/", "/v3/api-docs/",
            "/webjars/", "/static/", "/error/"
    };

    /**
     * Verifica o bucket de tokens do IP da requisição e permite ou bloqueia o acesso.
     * Requisições de API recebem resposta JSON com status 429; requisições web
     * recebem o erro padrão do servlet quando o limite é atingido.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getAttribute("jakarta.servlet.error.request_uri") != null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket());

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isApiRequest(request)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");

            response.getWriter().write("""
                        {
                          "status": 429,
                          "error": "Too many requests. Try again later."
                        }
                    """);
            return;
        }

        response.sendError(429);
    }

    /**
     * Detecta se a requisição é de API verificando o prefixo do URI (/api/, /auth/)
     * ou o header Accept com valor application/json, para formatar corretamente a resposta 429.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return uri.startsWith("/api/")
                || uri.startsWith("/auth/")
                || (accept != null && accept.contains("application/json"));
    }

    /**
     * Verifica se a requisição deve ser ignorada pelo rate limit, desconsiderando
     * recursos estáticos e endpoints de erro para não contabilizá-los no bucket do IP.
     */
    private boolean shouldSkip(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri.startsWith("/error")) {
            return true;
        }

        for (String p : SKIP_PATHS) {
            if (uri.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extrai o endereço IP real do cliente, considerando situações com proxy ou load balancer
     * via header X-Forwarded-For; utiliza o IP remoto direto como fallback.
     */
    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Implementação do algoritmo token bucket para controle de rate limit por IP.
     * Mantém até 500 tokens disponíveis, reabastecendo ao máximo a cada 60 segundos.
     */
    static class Bucket {
        private final AtomicLong tokens = new AtomicLong(CAPACITY);
        private volatile long lastRefill = System.currentTimeMillis();

        /**
         * Tenta consumir um token do bucket. Retorna {@code true} se havia tokens
         * disponíveis e a requisição pode prosseguir, ou {@code false} se o limite foi atingido.
         */
        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        /**
         * Reabastece os tokens ao valor máximo (500) quando o intervalo de 60 segundos
         * desde o último reabastecimento for atingido.
         */
        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefill >= REFILL_INTERVAL_MS) {
                tokens.set(CAPACITY);
                lastRefill = now;
            }
        }
    }
}