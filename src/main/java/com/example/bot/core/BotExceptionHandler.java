package com.example.bot.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Component
public class BotExceptionHandler {
    public void handle(Update update, Exception e) {
        var id = (update == null ? null : update.getUpdateId());
        log.error("Error while processing updateId={}", id, e);
    }
}