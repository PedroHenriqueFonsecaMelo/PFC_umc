package umc.exs.config;

import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configura o pool de threads para execução de tarefas agendadas (schedulers),
 * com 5 threads e shutdown gracioso aguardando tarefas em andamento por 30
 * segundos.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SmartLifecycle {

    private final ThreadPoolTaskScheduler scheduler;
    private boolean isRunning = false;

    /** Inicializa o scheduler com pool de 5 threads e prefixo "email-scheduler-". */
    public SchedulerConfig() {
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(5);
        this.scheduler.setThreadNamePrefix("email-scheduler-");

        this.scheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.scheduler.setAwaitTerminationSeconds(30);
    }

    /** Expõe o scheduler como bean para injeção nos schedulers da aplicação. */
    @Bean
    public TaskScheduler taskScheduler() {
        return this.scheduler;
    }

    /** Inicializa o scheduler no startup. */
    @Override
    public void start() {
        this.scheduler.initialize();
        this.isRunning = true;
    }

    /** Encerra o scheduler no shutdown. */
    @Override
    public void stop() {
        this.scheduler.shutdown();
        this.isRunning = false;
    }

    /** Retorna se o scheduler está ativo. */
    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /** Define que o scheduler é o último componente a parar no shutdown (Integer.MAX_VALUE). */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}