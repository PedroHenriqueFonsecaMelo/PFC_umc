package umc.exs.security;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Simple in-memory rate limiter: 10 requests/min per IP on auth paths.
 * Token bucket algo (refill 10 tokens/min).
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int CAPACITY = 10;
    private static final long REFILL_INTERVAL_MS = 60_000; // 1 min
    private static final String[] PROTECTED_PATHS = { "/auth/", "/clientes/login", "/api/login" };

    private boolean isProtected(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return false;
        String path = request.getRequestURI();
        for (String p : PROTECTED_PATHS) {
            if (path.startsWith(p))
                return true;
        }
        return false;
    }

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isProtected(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> new Bucket());

        if (bucket.tryConsume()) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("{\"error\": \"Too many requests. Try again later.\"}");
            log.warn("Rate limited IP: {}", ip);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty())
            return xf.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    static class Bucket {
        private final AtomicLong tokens = new AtomicLong(CAPACITY);
        private volatile long lastRefill = System.currentTimeMillis();

        public synchronized boolean tryConsume() {
            refill();
            return tokens.get() > 0 && tokens.decrementAndGet() >= 0;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            if (now - lastRefill >= REFILL_INTERVAL_MS) {
                tokens.set(CAPACITY);
                lastRefill = now;
            }
        }
    }
}
