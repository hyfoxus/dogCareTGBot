package com.example.bot.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Getter @Setter
@Validated
@ConfigurationProperties(prefix = "bot.handler-order")
public class HandlerOrderProperties {

    /**
     * Ключ — техническое имя хэндлера (например, "help", "start"),
     * значение — приоритет (меньше число = раньше).
     */
    private final Map<String, @Min(1) @Max(10000) Integer> orders = new LinkedHashMap<>();

    /** Безопасно получить порядок с дефолтом. */
    public int orderOf(String key, int def) {
        return orders.getOrDefault(key, def);
    }

    @PostConstruct
    void logConfiguredOrders() {
        log.info("Handler orders: {}", orders);
    }
}