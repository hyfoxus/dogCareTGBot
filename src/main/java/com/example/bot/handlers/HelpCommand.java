package com.example.bot.handlers;

import com.example.bot.core.UpdateHandler;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@org.springframework.core.annotation.Order(20)
@RequiredArgsConstructor
public class HelpCommand implements UpdateHandler {
    private final TelegramClient client;
    @Override public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText()
                && u.getMessage().getText().trim().startsWith("/help");
    }
    @Override public void handle(Update u) throws TelegramApiException {
        long chatId = u.getMessage().getChatId();
        String text = "Команды:\n/start — меню\n/help — помощь";
        client.execute(Reply.text(chatId, text));
    }
}
