package umc.exs.service.log;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* umc.exs..controller..*(..)) || execution(* umc.exs..service..*(..))")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long start = System.currentTimeMillis();

        String method = joinPoint.getSignature().toShortString();

        log.info("ENTER {}", method);

        try {
            Object result = joinPoint.proceed();

            long time = System.currentTimeMillis() - start;

            log.info("EXIT {} ({}ms)", method, time);

            if (time > 500) {
                log.warn("MÉTODO LENTO {} ({}ms)", method, time);
            }

            return result;

        } catch (Exception e) {
            log.error("ERRO {} -> {}", method, e.getMessage());
            throw e;
        }
    }
}