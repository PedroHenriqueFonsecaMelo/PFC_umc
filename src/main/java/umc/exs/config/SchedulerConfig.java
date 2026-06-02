package umc.exs.config;

import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class SchedulerConfig implements SmartLifecycle {

    private final ThreadPoolTaskScheduler scheduler;
    private boolean isRunning = false;

    public SchedulerConfig() {
        this.scheduler = new ThreadPoolTaskScheduler();
        this.scheduler.setPoolSize(5);
        this.scheduler.setThreadNamePrefix("email-scheduler-");

        this.scheduler.setWaitForTasksToCompleteOnShutdown(true);
        this.scheduler.setAwaitTerminationSeconds(30);
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return this.scheduler;
    }

    @Override
    public void start() {
        this.scheduler.initialize();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.scheduler.shutdown();
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}