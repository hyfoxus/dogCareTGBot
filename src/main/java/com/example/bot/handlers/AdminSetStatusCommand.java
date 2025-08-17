package com.example.bot.handlers;

import com.example.bot.core.UpdateHandler;
import com.example.bot.orders.OrderService;
import com.example.bot.orders.OrderStatus;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Order(15)
@RequiredArgsConstructor
@Slf4j
public class AdminSetStatusCommand implements UpdateHandler {

    private final TelegramClient client;
    private final OrderService orders;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText()
                && u.getMessage().getText().trim().startsWith("/set_status");
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        long chatId = u.getMessage().getChatId();
        String[] parts = u.getMessage().getText().trim().split("\\s+");
        if (parts.length < 3) {
            client.execute(Reply.text(chatId,
                    "Usage: /set_status <orderId> <status>\n" +
                            "Statuses: DRAFT, NEW, IN_PROGRESS, COMPLETED, CANCELED"));
            return;
        }
        String orderId = parts[1];
        String statusRaw = parts[2];

        OrderStatus status;
        try {
            status = OrderStatus.valueOf(statusRaw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            client.execute(Reply.text(chatId,
                    "Unknown status: " + statusRaw + "\nAllowed: DRAFT, NEW, IN_PROGRESS, COMPLETED, CANCELED"));
            return;
        }

        var opt = orders.updateStatus(orderId, status);
        if (opt.isPresent()) {
            client.execute(Reply.text(chatId, "OK: status of " + orderId + " -> " + status));
        } else {
            client.execute(Reply.text(chatId, "Order not found: " + orderId));
        }
    }
}