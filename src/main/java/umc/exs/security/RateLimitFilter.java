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

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private static final int CAPACITY = 100;
    private static final long REFILL_INTERVAL_MS = 60_000;

    private static final String[] SKIP_PATHS = {
            "/css/", "/js/", "/images/", "/favicon.ico", "/error",
            "/actuator/", "/h2-console/", "/swagger-ui/", "/v3/api-docs/",
            "/webjars/", "/static/", "/error/"
    };

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

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return uri.startsWith("/api/")
                || uri.startsWith("/auth/")
                || (accept != null && accept.contains("application/json"));
    }

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

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static class Bucket {
        private final AtomicLong tokens = new AtomicLong(CAPACITY);
        private volatile long lastRefill = System.currentTimeMillis();

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
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