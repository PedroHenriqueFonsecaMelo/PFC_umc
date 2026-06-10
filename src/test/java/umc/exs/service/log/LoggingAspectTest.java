package umc.exs.service.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

class LoggingAspectTest {

    LoggingAspect aspect = new LoggingAspect();

    @Test
    void logExecutionTime_quandoExecutaNormal_retornaResultado() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("metodo()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenReturn("OK");

        Object result = aspect.logExecutionTime(joinPoint);

        assertEquals("OK", result);
        verify(joinPoint).proceed();
    }

    @Test
    void logExecutionTime_quandoErro_lancaExcecao() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("metodo()");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("Erro"));

        assertThrows(RuntimeException.class, () -> {
            aspect.logExecutionTime(joinPoint);
        });
    }

    @Test
    void logExecutionTime_quandoMetodoLento_executaSemErro() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);

        when(signature.toShortString()).thenReturn("metodo()");
        when(joinPoint.getSignature()).thenReturn(signature);

        when(joinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(600);
            return "OK";
        });

        Object result = aspect.logExecutionTime(joinPoint);

        assertEquals("OK", result);
    }
}