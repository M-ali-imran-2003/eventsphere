package com.example.eventsphere.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    // Pull the URL from your application.properties
    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Bean
    public LettuceClientConfigurationBuilderCustomizer customizer() {
        return builder -> {
            // If the URL is secure (Heroku), we must disable peer verification
            // so Java doesn't panic over Heroku's self-signed certificates.
            if (redisUrl.startsWith("rediss://")) {
                builder.useSsl().disablePeerVerification();
            }
        };
    }
}