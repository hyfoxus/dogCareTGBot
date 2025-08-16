package com.example.bot.handlers;

import com.example.bot.config.BotProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.orders.OrderService;
import com.example.bot.orders.OrderStatus;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@org.springframework.core.annotation.Order(25)
@RequiredArgsConstructor
public class AdminSetStatusCommand implements UpdateHandler {

    private final TelegramClient client;
    private final OrderService orders;
    private final BotProperties props;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText()
                && u.getMessage().getText().startsWith("/setstatus");
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        var from = u.getMessage().getFrom();
        Long uid = from != null ? from.getId() : null;
        if (props.getAllowedUserIds()!=null && !props.getAllowedUserIds().isEmpty()
                && (uid==null || !props.getAllowedUserIds().contains(uid))) {
            client.execute(Reply.text(u.getMessage().getChatId(), "Нет прав"));
            return;
        }
        String[] p = u.getMessage().getText().trim().split("\s+");
        if (p.length != 3) {
            client.execute(Reply.text(u.getMessage().getChatId(),
                    "Формат: /setstatus <orderId> <NEW|IN_PROGRESS|COMPLETED|CANCELED>"));
            return;
        }
        String id = p[1];
        OrderStatus st;
        try { st = OrderStatus.valueOf(p[2]); }
        catch (IllegalArgumentException e) {
            client.execute(Reply.text(u.getMessage().getChatId(), "Неизвестный статус"));
            return;
        }
        boolean ok = orders.updateStatus(id, st);
        client.execute(Reply.text(u.getMessage().getChatId(), ok ? "OK" : "Заявка не найдена"));
    }
}
