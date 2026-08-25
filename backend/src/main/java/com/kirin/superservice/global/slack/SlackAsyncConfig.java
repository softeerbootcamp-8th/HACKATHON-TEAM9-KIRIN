package com.kirin.superservice.global.slack;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(SlackAsyncProperties.class)
public class SlackAsyncConfig {

    private final SlackAsyncProperties properties;

    public SlackAsyncConfig(SlackAsyncProperties properties) {
        this.properties = properties;
    }

    @Bean(name = "slackNotificationExecutor")
    public ThreadPoolTaskExecutor slackNotificationExecutor() {
        SlackAsyncProperties.Pool pool = properties.slackNotification();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(pool.corePoolSize());
        executor.setMaxPoolSize(pool.maxPoolSize());
        executor.setQueueCapacity(pool.queueCapacity());
        executor.setThreadNamePrefix("slack-notify-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
