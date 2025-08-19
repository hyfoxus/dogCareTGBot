package com.example.bot.orders;

import com.example.bot.jpa.OrderPersistence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    /** Основное оперативное хранилище заказов (без Redis). */
    private final ConcurrentMap<String, Order> store = new ConcurrentHashMap<>();

    /** Индекс активного черновика по чату. */
    private final ConcurrentMap<Long, String> activeDraftByChat = new ConcurrentHashMap<>();

    /** Персист на H2 (как и раньше). */
    private final OrderPersistence orderPersistence;

    /** Возвращает id активного черновика для чата, если есть. */
    public Optional<String> getActiveDraftId(Long chatId) {
        return Optional.ofNullable(activeDraftByChat.get(chatId));
    }

    /**
     * Создаёт/обновляет черновик.
     * Если existingOrderIdOrNull пуст — создаём новый DRAFT.
     * Если передан description/status — применяем к объекту.
     */
    public Order beginOrUpdateDraft(Long chatId,
                                    String service,
                                    String subtype,
                                    String descriptionOrNull,
                                    OrderStatus statusOrNull,
                                    String existingOrderIdOrNull) {

        // 1) загрузить существующий (из in-memory) или создать новый
        Order o = null;
        if (existingOrderIdOrNull != null && !existingOrderIdOrNull.isBlank()) {
            o = store.get(existingOrderIdOrNull);
        }
        if (o == null) {
            o = new Order();
            o.setId(UUID.randomUUID().toString());
            o.setChatId(chatId);
            o.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            o.setStatus(statusOrNull != null ? statusOrNull : OrderStatus.DRAFT);
        }

        // 2) применить изменения
        o.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (service != null) o.setService(service);
        if (subtype != null) o.setSubtype(subtype);
        if (descriptionOrNull != null) o.setDescription(descriptionOrNull);
        if (statusOrNull != null) o.setStatus(statusOrNull);

        // 3) сохранить
        Order saved = save(o);

        // 4) обновить указатель на черновик
        if (saved.getStatus() == OrderStatus.DRAFT) {
            activeDraftByChat.put(chatId, saved.getId());
        } else {
            // финализирован — черновик больше не актуален
            activeDraftByChat.compute(chatId, (k, v) -> (saved.getId().equals(v) ? null : v));
        }
        return saved;
    }

    /** Универсальное сохранение заказа: in-memory + H2. */
    public Order save(Order o) {
        if (o.getId() == null || o.getId().isBlank()) {
            o.setId(UUID.randomUUID().toString());
        }
        if (o.getCreatedAt() == null) {
            o.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (o.getUpdatedAt() == null) {
            o.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }

        store.put(o.getId(), o);
        // write-through в H2
        try {
            orderPersistence.saveFromModel(o);
        } catch (Exception ex) {
            log.warn("H2 persistence failed for order {}", o.getId(), ex);
        }

        if (log.isDebugEnabled()) {
            log.debug("Saved order {} status={} chat={}", o.getId(), o.getStatus(), o.getChatId());
        }
        return o;
    }

    /** Получить заказ по id из in-memory. */
    public Optional<Order> findById(String orderId) {
        if (orderId == null || orderId.isBlank()) return Optional.empty();
        return Optional.ofNullable(store.get(orderId));
    }

    /** Последние N заказов (по дате создания), глобально. */
    public List<Order> latest(int limit) {
        int n = Math.max(1, Math.min(200, limit));
        return store.values().stream()
                .sorted(Comparator.comparing(Order::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Последние N заказов по конкретному чату. */
    public List<Order> latestByChat(Long chatId, int limit) {
        int n = Math.max(1, Math.min(200, limit));
        return store.values().stream()
                .filter(o -> Objects.equals(o.getChatId(), chatId))
                .sorted(Comparator.comparing(Order::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /**
     * Отмена черновика по его orderId.
     * Удаляет объект из in-memory, чистит указатель черновика и удаляет запись из H2.
     */
    public void cancelDraft(String orderId) {
        if (orderId == null || orderId.isBlank()) return;

        Order removed = store.remove(orderId);
        if (removed == null) return;

        // убрать указатель на черновик, если он на нас указывает
        activeDraftByChat.compute(removed.getChatId(), (k, v) -> (orderId.equals(v) ? null : v));

        // удалить из H2
        try {
            orderPersistence.deleteById(orderId);
        } catch (Exception ex) {
            log.warn("H2 delete failed for order {}", orderId, ex);
        }

        if (log.isDebugEnabled()) {
            log.debug("Draft canceled and deleted: {} (chat={})", orderId, removed.getChatId());
        }
    }

    /** Обратная совместимость: отменить черновик по chatId. */
    public void cancelDraft(Long chatId) {
        String draftId = activeDraftByChat.get(chatId);
        if (draftId != null) {
            cancelDraft(draftId);
        }
        if (log.isDebugEnabled()) {
            log.debug("Cancel draft requested for chat={}", chatId);
        }
    }

    /** Обновление статуса с корректной работой указателя черновика. */
    public Optional<Order> updateStatus(String orderId, OrderStatus newStatus) {
        if (orderId == null || orderId.isBlank() || newStatus == null) {
            return Optional.empty();
        }
        Order updated = store.computeIfPresent(orderId, (id, o) -> {
            o.setStatus(newStatus);
            o.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            if (newStatus == OrderStatus.DRAFT) {
                activeDraftByChat.put(o.getChatId(), o.getId());
            } else {
                activeDraftByChat.compute(o.getChatId(), (k, v) -> (o.getId().equals(v) ? null : v));
            }
            return o;
        });

        if (updated == null) {
            log.warn("updateStatus: order {} not found", orderId);
            return Optional.empty();
        }

        // персист в H2
        try {
            orderPersistence.saveFromModel(updated);
        } catch (Exception ex) {
            log.warn("H2 persistence failed on updateStatus for order {}", orderId, ex);
        }
        return Optional.of(updated);
    }
}