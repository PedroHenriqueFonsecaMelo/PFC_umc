package umc.exs.handler;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Object handleBusiness(BusinessException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("Business error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", ex.getMessage()));
        }

        response.setStatus(400);
        return "error/400";
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public Object handle429(TooManyRequestsException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("429 error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.status(429).body(Map.of(
                    "status", 429,
                    "error", ex.getMessage()));
        }

        response.setStatus(429);
        return "error/429";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handle404(NoResourceFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.warn("404 error: {}", ex.getMessage());

        if (isRest(request)) {
            return ResponseEntity.status(404).body(Map.of(
                    "status", 404,
                    "error", "Not found"));
        }

        response.setStatus(404);
        return "error/404";
    }

    @ExceptionHandler(Exception.class)
    public Object handle500(Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.error("500 error", ex);

        if (isRest(request)) {
            return ResponseEntity.status(500).body(Map.of(
                    "status", 500,
                    "error", "Internal error"));
        }

        response.setStatus(500);
        return "error/500";
    }

    private boolean isRest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        return uri.startsWith("/api/")
                || uri.startsWith("/auth/")
                || (accept != null && accept.contains("application/json"));
    }
}