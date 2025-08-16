package com.example.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "telegram.flow")
public class FlowProperties {
    private String dispatcherContact;
    private String jobFormUrl;
    private Long adminChatId;
}