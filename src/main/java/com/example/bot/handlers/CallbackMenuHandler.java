package com.example.bot.handlers;

import com.example.bot.config.FlowProperties;
import com.example.bot.config.UiProperties;
import com.example.bot.core.UpdateHandler;
import com.example.bot.flow.FlowState;
import com.example.bot.flow.SessionService;
import com.example.bot.orders.Order;
import com.example.bot.orders.OrderService;
import com.example.bot.util.Reply;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static java.util.List.of;

@Component
@org.springframework.core.annotation.Order(20)
@RequiredArgsConstructor
public class CallbackMenuHandler implements UpdateHandler {

    private final TelegramClient client;
    private final OrderService orderService;
    private final SessionService sessionService;
    private final UiProperties ui;
    private final FlowProperties flow;

    @Override
    public boolean supports(Update u) {
        return u.hasCallbackQuery();
    }

    @Override
    public void handle(Update u) throws TelegramApiException {
        var cq = u.getCallbackQuery();
        long chatId = cq.getMessage().getChatId();
        String data = cq.getData();

        switch (data) {
            case "SERVICES" -> showServices(chatId);
            case "BACK_MAIN" -> new StartCommand(client, ui).handle(u);
            case "S_WALK" -> showWalk(chatId);
            case "WALK_NORMAL" -> askDescription(chatId, "Выгул", "Обычный");
            case "WALK_ACTIVE" -> askDescription(chatId, "Выгул", "Активный");
            case "CANCEL_DRAFT" -> {
                var s = sessionService.get(chatId);
                var draftId = s.getCurrentOrderId();
                if (draftId != null) {
                    orderService.cancelDraft(draftId);
                }
                s.setState(FlowState.IDLE);
                s.setCurrentOrderId(null);
                sessionService.save(s);
                client.execute(Reply.text(chatId, "Черновик удалён. Вы в главном меню."));
                new StartCommand(client, ui).handle(u);
            }
            case "CALL_MANAGER" -> sendContact(chatId);
            case "GENERAL" -> showGeneralFaq(chatId);
            default -> client.execute(Reply.text(chatId, "Неизвестная команда. Вернуться в /start?"));
        }
    }

    private void showServices(long chatId) throws TelegramApiException {
        var b = ui.getServicesMenu().getButtons();
        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn(b.getWalk(), "S_WALK")),
                Reply.row(Reply.btn(b.getBoarding(), "S_BOARDING")),
                Reply.row(Reply.btn(b.getNanny(), "S_NANNY")),
                Reply.row(Reply.btn(b.getBack(), "BACK_MAIN"))
        ));
        client.execute(Reply.text(chatId, ui.getServicesMenu().getTitle(), kb));
    }

    private void showWalk(long chatId) throws TelegramApiException {
        var b = ui.getWalkMenu().getButtons();
        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn(b.getNormal(), "WALK_NORMAL"), Reply.btn(b.getActive(), "WALK_ACTIVE")),
                Reply.row(Reply.btn(b.getBack(), "SERVICES")),
                Reply.row(Reply.btn(b.getCancel(), "CANCEL_DRAFT"))
        ));
        client.execute(Reply.text(chatId, ui.getWalkMenu().getTitle(), kb));
    }

    private void askDescription(long chatId, String service, String subtype) throws TelegramApiException {
        Order draft = orderService.beginOrUpdateDraft(chatId, service, subtype, null, null, null);

        var s = sessionService.get(chatId);
        s.setState(FlowState.AWAITING_DESCRIPTION);
        s.setService(draft.getService());
        s.setSubtype(draft.getSubtype());
        s.setCurrentOrderId(draft.getId());
        sessionService.save(s);

        String subtypeSuffix = draft.getSubtype() != null ? " • " + draft.getSubtype() : "";
        String header = ui.getMessages().getDraftHeader()
                .replace("{id}", draft.getId())
                .replace("{service}", draft.getService())
                .replace("{subtypeSuffix}", subtypeSuffix);

        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn("⬅️ В меню", "BACK_MAIN")),
                Reply.row(Reply.btn(ui.getWalkMenu().getButtons().getCancel(), "CANCEL_DRAFT")),
                Reply.row(Reply.btn("📲 Связаться с диспетчером", "CALL_MANAGER"))
        ));

        client.execute(Reply.text(chatId, header + "\n\n" + ui.getMessages().getDraftTip(), kb));
    }

    private void sendContact(long chatId) throws TelegramApiException {
        String contact = flow.getDispatcherContact(); // например, "@dog_dispatcher"
        String form = flow.getJobFormUrl();           // например, ссылка на анкету
        StringBuilder sb = new StringBuilder();
        if (contact != null && !contact.isBlank()) {
            sb.append("Диспетчер: ").append(contact).append("\n");
        }
        if (form != null && !form.isBlank()) {
            sb.append("Анкета/форма: ").append(form).append("\n");
        }
        if (sb.length() == 0) {
            sb.append("Свяжитесь с нами в чате — поможем подобрать услугу.");
        }
        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn("⬅️ Назад", "BACK_MAIN"))
        ));
        client.execute(Reply.text(chatId, sb.toString(), kb));
    }

    private void showGeneralFaq(long chatId) throws TelegramApiException {
        var f = ui.getFaq();
        InlineKeyboardMarkup kb = Reply.buttons(of(
                Reply.row(Reply.btn("Стоимость", "GQ_COST"), Reply.btn("Оплата", "GQ_PAY")),
                Reply.row(Reply.btn("Ключи", "GQ_KEYS"), Reply.btn("Аптечка", "GQ_MEDKIT")),
                Reply.row(Reply.btn("Мытьё лап", "GQ_WASHPAWS"), Reply.btn("Кормление", "GQ_FEED")),
                Reply.row(Reply.btn("Договор", "GQ_CONTRACT")),
                Reply.row(Reply.btn("⬅️ Назад", "BACK_MAIN"))
        ));
        client.execute(Reply.text(chatId, "Частые вопросы:", kb));

        // Обработка конкретных GQ_* может быть в этом же классе (через supports/handle) или отдельным
        // Для простоты: пусть пользователь нажимает — вернём текст в другом месте или добавь switch здесь.
    }
}