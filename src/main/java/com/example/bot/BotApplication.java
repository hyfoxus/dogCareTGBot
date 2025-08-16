package com.example.bot;

import com.example.bot.config.UiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.bot.config.BotProperties;
import com.example.bot.config.FlowProperties;
import com.example.bot.config.WebhookProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        BotProperties.class, FlowProperties.class, WebhookProperties.class, UiProperties.class
})
public class BotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
