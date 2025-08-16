package com.example.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram.webhook")
public class WebhookProperties {
    private String url;
    private String secretToken;
    private String path = "/webhook/telegram";
}