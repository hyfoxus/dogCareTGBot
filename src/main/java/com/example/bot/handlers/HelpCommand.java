package com.example.bot.handlers;

import com.example.bot.config.HandlerOrderProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@RequiredArgsConstructor
public class HelpCommand implements UpdateHandler {

    private final TelegramClient client;
    private final HandlerOrderProperties orderProps;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText()
                && u.getMessage().getText().trim().startsWith("/help");
    }

    /** Порядок берём из конфига: bot.handler-order.help (дефолт 20). */
    @Override
    public int order() {
        return orderProps.orderOf("help", 20);
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        long chatId = u.getMessage().getChatId();
        String text = "Команды:\n/start — меню\n/help — помощь";
        client.execute(Reply.text(chatId, text));
    }
}