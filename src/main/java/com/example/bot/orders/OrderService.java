package com.example.bot.orders;

import com.example.bot.jpa.OrderPersistence;
import com.example.bot.orders.OrderStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    // ===== Redis keys/names =====
    private static final String KEY_PREFIX_ORDER = "order:";                    // order:<id>
    private static final String KEY_DRAFT_PREFIX = "orders:draft:chat:";        // orders:draft:chat:<chatId>
    private static final String IDX_ALL = "orders:index:all";                   // ZSET of ids
    private static final String IDX_BY_CHAT_PREFIX = "orders:index:chat:";      // orders:index:chat:<chatId>

    private final StringRedisTemplate srt;
    private final ObjectMapper mapper;              // Spring'овский ObjectMapper
    private final OrderPersistence orderPersistence; // H2 persistence

    // ===== Public API =====

    /** Возвращает id активного черновика для чата, если есть. */
    public Optional<String> getActiveDraftId(Long chatId) {
        String id = srt.opsForValue().get(draftKey(chatId));
        return Optional.ofNullable(id).filter(s -> !s.isBlank());
    }

    /**
     * Создаёт или обновляет черновик. Если {@code existingOrderIdOrNull} не задан,
     * будет создан новый заказ в статусе DRAFT (если {@code statusOrNull} не указан).
     * Если передан {@code descriptionOrNull} и/или {@code statusOrNull}, они применяются.
     */
    public Order beginOrUpdateDraft(Long chatId,
                                    String service,
                                    String subtype,
                                    String descriptionOrNull,
                                    OrderStatus statusOrNull,
                                    String existingOrderIdOrNull) {

        // 1) загрузить существующий или создать новый
        Order o = null;
        if (existingOrderIdOrNull != null && !existingOrderIdOrNull.isBlank()) {
            o = findById(existingOrderIdOrNull).orElse(null);
        }
        if (o == null) {
            o = new Order();
            o.setId(java.util.UUID.randomUUID().toString());
            o.setChatId(chatId);
            o.setCreatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            o.setStatus(statusOrNull != null ? statusOrNull : OrderStatus.DRAFT);
        }

        // 2) применить изменения
        o.setUpdatedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
        if (service != null) o.setService(service);
        if (subtype != null) o.setSubtype(subtype);
        if (descriptionOrNull != null) o.setDescription(descriptionOrNull);
        if (statusOrNull != null) o.setStatus(statusOrNull);

        // 3) сохранить
        Order saved = save(o);

        // 4) обновить указатель на черновик для чата (если статус DRAFT)
        if (saved.getStatus() == OrderStatus.DRAFT) {
            srt.opsForValue().set(draftKey(chatId), saved.getId());
        } else {
            // финализирован — черновик больше не актуален
            srt.delete(draftKey(chatId));
        }
        return saved;
    }

    /** Универсальное сохранение заказа: Redis (+индексы) + H2. */
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

        // Redis: сам объект
        String json = toJson(o);
        srt.opsForValue().set(orderKey(o.getId()), json);

        // Redis: индексы (по времени создания)
        touchIndexes(o);

        // H2: персистенция
        orderPersistence.saveFromModel(o);

        if (log.isDebugEnabled()) {
            log.debug("Saved order {} status={} chat={}", o.getId(), o.getStatus(), o.getChatId());
        }
        return o;
    }

    /** Получить заказ по id из Redis. */
    public Optional<Order> findById(String orderId) {
        if (orderId == null || orderId.isBlank()) return Optional.empty();
        String json = srt.opsForValue().get(orderKey(orderId));
        if (json == null) return Optional.empty();
        return Optional.ofNullable(fromJson(json));
    }

    /** Последние N заказов (по дате создания), глобально. */
    public List<Order> latest(int limit) {
        int n = Math.max(1, Math.min(200, limit));
        Set<String> ids = srt.opsForZSet().reverseRange(IDX_ALL, 0, n - 1);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    /** Последние N заказов по конкретному чату. */
    public List<Order> latestByChat(Long chatId, int limit) {
        int n = Math.max(1, Math.min(200, limit));
        Set<String> ids = srt.opsForZSet().reverseRange(chatIndexKey(chatId), 0, n - 1);
        if (ids == null || ids.isEmpty()) return List.of();
        return ids.stream()
                .map(this::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Отмена черновика по его orderId.
     * Удаляет объект, чистит указатель черновика и индексы.
     * Удаляет запись и из H2.
     */
    public void cancelDraft(String orderId) {
        if (orderId == null || orderId.isBlank()) return;

        findById(orderId).ifPresent(o -> {
            // удалить сам заказ в Redis
            srt.delete(orderKey(orderId));

            // убрать указатель на черновик для этого чата, если он на нас указывает
            String draftPtrKey = draftKey(o.getChatId());
            String currentPtr = srt.opsForValue().get(draftPtrKey);
            if (orderId.equals(currentPtr)) {
                srt.delete(draftPtrKey);
            }

            // убрать из индексов
            removeFromIndexes(o);

            // H2: удалить запись
            orderPersistence.deleteById(orderId);

            if (log.isDebugEnabled()) {
                log.debug("Draft canceled and deleted: {} (chat={})", orderId, o.getChatId());
            }
        });
    }

    /** Обратная совместимость: отменить черновик по chatId. */
    public void cancelDraft(Long chatId) {
        getActiveDraftId(chatId).ifPresent(this::cancelDraft);
        if (log.isDebugEnabled()) {
            log.debug("Cancel draft requested for chat={}", chatId);
        }
    }

    // ===== Private helpers =====

    private void touchIndexes(Order o) {
        double score = score(o.getCreatedAt());
        srt.opsForZSet().add(IDX_ALL, o.getId(), score);
        srt.opsForZSet().add(chatIndexKey(o.getChatId()), o.getId(), score);
    }

    private void removeFromIndexes(Order o) {
        srt.opsForZSet().remove(IDX_ALL, o.getId());
        srt.opsForZSet().remove(chatIndexKey(o.getChatId()), o.getId());
    }

    private static double score(OffsetDateTime odt) {
        // секунды epoch; для стабильности сортировки можно добавить небольшую фракцию из nanos
        long secs = (odt != null ? odt.toEpochSecond() : OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());
        return (double) secs;
    }

    private static String orderKey(String id) {
        return KEY_PREFIX_ORDER + id;
    }

    private static String draftKey(Long chatId) {
        return KEY_DRAFT_PREFIX + chatId;
    }

    private static String chatIndexKey(Long chatId) {
        return IDX_BY_CHAT_PREFIX + chatId;
    }

    private String toJson(Order o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize order " + o.getId(), e);
        }
    }

    private Order fromJson(String json) {
        try {
            return mapper.readValue(json, Order.class);
        } catch (Exception e) {
            log.warn("Failed to deserialize order JSON: {}", json, e);
            return null;
        }
    }
    public Optional<Order> updateStatus(String orderId, OrderStatus newStatus) {
        if (orderId == null || orderId.isBlank() || newStatus == null) {
            return Optional.empty();
        }
        Optional<Order> updated = findById(orderId).map(o -> {
            o.setStatus(newStatus);
            o.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

            // Если уход с DRAFT → удалить указатель черновика,
            // если приход в DRAFT → установить указатель.
            if (newStatus == OrderStatus.DRAFT) {
                srt.opsForValue().set(draftKey(o.getChatId()), o.getId());
            } else {
                String ptrKey = draftKey(o.getChatId());
                String curr = srt.opsForValue().get(ptrKey);
                if (o.getId().equals(curr)) {
                    srt.delete(ptrKey);
                }
            }

            return save(o); // save() обновит Redis + индексы + H2
        });

        if (updated.isEmpty()) {
            log.warn("updateStatus: order {} not found", orderId);
        }
        return updated;
    }
}