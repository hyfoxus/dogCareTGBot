package com.example.bot.core;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {
    boolean supports(Update update);
    void handle(Update update) throws Exception;
    default int order() { return 0; }
}
