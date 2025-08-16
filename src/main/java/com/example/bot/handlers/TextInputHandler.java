package com.example.bot.handlers;

import com.example.bot.config.UiProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.flow.FlowState;
import com.example.bot.flow.SessionService;
import com.example.bot.orders.Order;
import com.example.bot.orders.OrderService;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Component
@org.springframework.core.annotation.Order(40)
@RequiredArgsConstructor
@Slf4j
public class TextInputHandler implements UpdateHandler {

    private final TelegramClient client;
    private final SessionService sessionService;
    private final OrderService orderService;
    private final UiProperties ui;

    @Override
    public boolean supports(Update u) {
        return u.hasMessage() && u.getMessage().hasText();
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        long chatId = u.getMessage().getChatId();
        String text = u.getMessage().getText().trim();

        var s = sessionService.get(chatId);
        if (s.getState() != FlowState.AWAITING_DESCRIPTION) {
            return; // это не наш кейс — пусть обработает другой handler
        }

        // Обновляем черновик описанием и переводим в статус NEW (или SUBMITTED — по вашей логике)
        Order finalOrder = orderService.beginOrUpdateDraft(
                chatId,
                s.getService(),
                s.getSubtype(),
                text,
                "NEW",
                s.getCurrentOrderId()
        );

        // Сбрасываем сессию в IDLE
        s.setState(FlowState.IDLE);
        s.setDescriptionDraft(null);
        s.setCurrentOrderId(null);
        sessionService.save(s);

        // Итоговое сообщение
        String subtypeSuffix = finalOrder.getSubtype() != null ? " • " + finalOrder.getSubtype() : "";
        String summary = ui.getMessages().getSummary()
                .replace("{id}", finalOrder.getId())
                .replace("{status}", String.valueOf(finalOrder.getStatus()))
                .replace("{service}", finalOrder.getService())
                .replace("{subtypeSuffix}", subtypeSuffix)
                .replace("{description}", finalOrder.getDescription());

        var kb = Reply.buttons(List.of(
                Reply.row(Reply.btn("⬅️ В меню", "BACK_MAIN"))
        ));

        client.execute(Reply.text(chatId, summary, kb));
    }
}