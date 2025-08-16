package com.example.bot.webhook;

import com.example.bot.config.WebhookProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRegistrar {

    private final TelegramClient client;
    private final WebhookProperties props;

    @PostConstruct
    public void register() {
        if (props.getUrl() == null || props.getUrl().isBlank()) {
            log.warn("telegram.webhook.url is empty — webhook will not be registered");
            return;
        }
        try {
            var req = SetWebhook.builder()
                    .url(props.getUrl())
                    .secretToken(props.getSecretToken())
                    .build();
            client.execute(req);
            log.info("Webhook set to {}", props.getUrl());
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Failed to setWebhook: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void unregister() {
        try { client.execute(new DeleteWebhook()); } catch (Exception ignored) {}
    }
}