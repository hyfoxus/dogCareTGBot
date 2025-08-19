package com.example.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "telegram.bot")
public class BotProperties {
    private String token;
    private String username;
    private List<Long> allowedUserIds;
}