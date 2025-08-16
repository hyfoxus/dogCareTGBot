package com.example.bot.handlers;

import com.example.bot.config.BotProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.orders.Order;
import com.example.bot.orders.OrderService;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@org.springframework.core.annotation.Order(26)
@RequiredArgsConstructor
public class AdminListOrdersCommand implements UpdateHandler {
    private final TelegramClient client;
    private final OrderService orders;
    private final BotProperties props;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText()
                && u.getMessage().getText().startsWith("/orders");
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        Long uid = u.getMessage().getFrom()!=null ? u.getMessage().getFrom().getId() : null;
        if (props.getAllowedUserIds()!=null && !props.getAllowedUserIds().isEmpty()
                && (uid==null || !props.getAllowedUserIds().contains(uid))) {
            client.execute(Reply.text(u.getMessage().getChatId(), "Нет прав"));
            return;
        }
        String[] p = u.getMessage().getText().trim().split("\s+");
        int limit = (p.length>=2) ? Math.max(1, Math.min(50, Integer.parseInt(p[1]))) : 10;

        List<Order> list = orders.latest(limit);
        if (list.isEmpty()) {
            client.execute(Reply.text(u.getMessage().getChatId(), "Заявок пока нет"));
            return;
        }
        StringBuilder sb = new StringBuilder("*Последние заявки:*\n");
        for (Order o : list) {
            sb.append("• `").append(o.getId()).append("` ").append(o.getStatus()).append(" — ")
              .append(o.getService()).append(o.getSubtype()!=null?(" • "+o.getSubtype()):"")
              .append("\n");
        }
        client.execute(Reply.text(u.getMessage().getChatId(), sb.toString()));
    }
}
