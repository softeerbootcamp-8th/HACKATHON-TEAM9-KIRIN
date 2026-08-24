package com.kirin.superservice.global.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("async")
public record SlackAsyncProperties(Pool slackNotification) {

    public record Pool(int corePoolSize, int maxPoolSize, int queueCapacity) {
    }
}
