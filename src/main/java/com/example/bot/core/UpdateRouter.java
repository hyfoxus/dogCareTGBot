package com.example.bot.core;

import com.example.bot.config.BotProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateRouter {

    private final List<UpdateHandler> handlers;          // внедряются как есть
    private final BotExceptionHandler exceptionHandler;
    private final BotProperties props;
    private final TelegramClient client;

    // итоговый предсортированный и неизменяемый список
    private List<UpdateHandler> orderedHandlers;

    @PostConstruct
    void init() {
        this.orderedHandlers = List.copyOf(
                handlers.stream()
                        // порядок берётся из handler.order(), который читает значения из конфигурации
                        .sorted(Comparator
                                .comparingInt(UpdateHandler::order)
                                // стабильность при одинаковом order (необязательно, но полезно)
                                .thenComparing(h -> h.getClass().getName()))
                        .toList()
        );
        if (log.isInfoEnabled()) {
            log.info("UpdateRouter initialized with {} handlers:", orderedHandlers.size());
            for (UpdateHandler h : orderedHandlers) {
                log.info("  {} -> order={}", h.getClass().getSimpleName(), h.order());
            }
        }
    }

    public void consume(Update update) {
        final String info = brief(update);
        final long t0 = System.nanoTime();
        try {
            UpdateHandler target = null;
            for (UpdateHandler h : orderedHandlers) {    // уже предсортированный список
                if (supportsSafe(h, update)) {
                    target = h;
                    break;
                }
            }
            if (target != null) {
                log.info("Dispatch {} -> {}", info, target.getClass().getSimpleName());
                target.handle(update);
            } else {
                log.debug("No handler matched {}", info);
            }
        } catch (Exception e) {
            exceptionHandler.handle(update, e);
        } finally {
            long dt = System.nanoTime() - t0;
            log.debug("Route done {} in {} ms", info, dt / 1_000_000.0);
        }
    }

    private boolean supportsSafe(UpdateHandler h, Update u) {
        try {
            return h.supports(u);
        } catch (Exception ex) {
            log.warn("supports() threw in {}: {}", h.getClass().getSimpleName(), ex.toString());
            return false;
        }
    }

    private String brief(Update u) {
        if (u == null) return "update=null";
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("upd#").append(u.getUpdateId());
            if (u.hasCallbackQuery()) {
                var cq = u.getCallbackQuery();
                Long chatId = cq != null && cq.getMessage() != null ? cq.getMessage().getChatId() : null;
                String data = cq != null ? cq.getData() : null;
                sb.append(" cb chat=").append(chatId)
                        .append(" data=\"").append(safeText(data)).append("\"");
            } else if (u.hasMessage()) {
                var m = u.getMessage();
                sb.append(" msg chat=").append(m.getChatId());
                if (m.hasText()) {
                    sb.append(" text=\"").append(safeText(m.getText())).append("\"");
                }
            } else if (u.hasInlineQuery()) {
                sb.append(" inline q=\"").append(safeText(u.getInlineQuery().getQuery())).append("\"");
            } else {
                sb.append(" type=other");
            }
            return sb.toString();
        } catch (Exception ex) {
            return "upd#? (brief-failed)";
        }
    }

    private static String safeText(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() > 64) t = t.substring(0, 64) + "…";
        return t.replaceAll("[\\r\\n\\t]", " ");
    }

    public TelegramClient client() { return client; }
    public String getBotUsername() { return props.getUsername(); }
}