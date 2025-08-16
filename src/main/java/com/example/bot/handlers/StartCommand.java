package com.example.bot.handlers;

import com.example.bot.config.UiProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static java.util.List.of;

@Component
@org.springframework.core.annotation.Order(10)
@RequiredArgsConstructor
public class StartCommand implements UpdateHandler {

    private final TelegramClient client;
    private final UiProperties ui;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage()
                && u.getMessage().hasText()
                && "/start".equalsIgnoreCase(u.getMessage().getText().trim());
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        long chatId = u.getMessage().getChatId();

        var b = ui.getMainMenu().getButtons();
        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn(b.getServices(), "SERVICES"), Reply.btn(b.getWork(), "WORK")),
                Reply.row(Reply.btn(b.getCallManager(), "CALL_MANAGER"), Reply.btn(b.getGeneral(), "GENERAL"))
        ));

        client.execute(Reply.text(chatId, ui.getMainMenu().getTitle(), kb));
    }
}