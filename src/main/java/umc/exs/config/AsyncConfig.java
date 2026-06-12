package umc.exs.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
/**
 * Configura o executor assíncrono para envio de e-mails, com pool de 2 a 5
 * threads e fila de até 100 tarefas pendentes.
 */
@EnableAsync
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Cria o pool de threads dedicado ao envio assíncrono de e-mails, aguardando
     * tarefas pendentes por até 30 segundos no shutdown.
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-async-");
        // Aguarda e-mails pendentes antes de encerrar a aplicação
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * Captura e loga erros ocorridos em métodos assíncronos de envio de e-mail
     * sem propagar a exceção.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> org.slf4j.LoggerFactory.getLogger(AsyncConfig.class)
                .error("Falha ao enviar e-mail [método={}]: {}", method.getName(), ex.getMessage(), ex);
    }
}
