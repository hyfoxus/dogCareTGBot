package com.example.bot;

import com.example.bot.config.UiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.bot.config.BotProperties;
import com.example.bot.config.FlowProperties;
import com.example.bot.config.WebhookProperties;

@SpringBootApplication
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackageClasses = com.example.bot.jpa.OrderEntity.class)
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackageClasses = com.example.bot.orders.OrderRepository.class)
@ConfigurationPropertiesScan(basePackages = "com.example.bot.config")

public class BotApplication {
    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
