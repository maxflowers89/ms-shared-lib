package com.illimity.rts.commonconfiglib.configs.async;

import lombok.extern.log4j.Log4j2;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Log4j2
@Configuration
@EnableAsync
@ConditionalOnProperty(prefix = "async", name = "enabled", havingValue = "true")
public class AsyncConfig implements AsyncConfigurer {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    @Value("${async.corePoolSize:16}")
    private int corePoolSize;
    @Value("${async.maxPoolSize:64}")
    private int maxPoolSize;
    @Value("${async.queueCapacity:1024}")
    private int queueCapacity;

    @Bean
    public Executor taskExecutor() {
        return getAsyncExecutor();
    }

    @Override
    public Executor getAsyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Math.max(corePoolSize, AVAILABLE_PROCESSORS));
        executor.setMaxPoolSize(Math.max(maxPoolSize, AVAILABLE_PROCESSORS * 2));
        executor.setQueueCapacity(Math.max(queueCapacity, AVAILABLE_PROCESSORS * 8));
        executor.setTaskDecorator(new ThreadContextTaskDecorator());
        executor.setThreadNamePrefix("AsyncExec-");
        executor.initialize();

        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new CustomAsyncExceptionHandler();
    }
}
