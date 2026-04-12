package net.ddns.adambravo79.nextdns.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("NextDnsTimer-");
        scheduler.setErrorHandler(throwable -> 
            System.err.println("Erro no scheduler: " + throwable.getMessage()));
        scheduler.initialize();
        return scheduler;
    }
}