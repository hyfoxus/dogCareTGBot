package com.example.bot.core;

import com.example.bot.config.BotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateRouter {

    private final List<UpdateHandler> handlers;
    private final BotExceptionHandler exceptionHandler;
    private final BotProperties props;
    private final TelegramClient client;

    public void consume(Update update) {
        try {
            UpdateHandler target = handlers.stream()
                    .sorted(Comparator.comparingInt(UpdateHandler::order))
                    .filter(h -> h.supports(update))
                    .findFirst()
                    .orElse(null);
            if (target != null) {
                log.debug("Dispatching {} for update {}", target.getClass().getSimpleName(), update);
                target.handle(update);
            } else {
                log.debug("No handler found for update {}", update);
            }
        } catch (Exception e) {
            exceptionHandler.handle(update, e);
        }
    }

    public TelegramClient client() { return client; }
    public String getBotUsername() { return props.getUsername(); }
}