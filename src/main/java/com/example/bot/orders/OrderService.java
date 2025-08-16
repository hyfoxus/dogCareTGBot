package com.example.bot.orders;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final StringRedisTemplate srt;

    private static final String IDX_ALL = "orders:index:all";
    private static final String IDX_BY_CHAT_PREFIX = "orders:index:chat:";
    private static final String DRAFT_BY_CHAT_PREFIX = "orders:draft:chat:";

    private String draftKey(Long chatId) { return DRAFT_BY_CHAT_PREFIX + chatId; }

    public Optional<String> getActiveDraftId(Long chatId) {
        String id = srt.opsForValue().get(draftKey(chatId));
        return Optional.ofNullable(id).filter(s -> !s.isBlank());
    }

    public Order beginOrUpdateDraft(Long chatId, String service, String subtype,
                                    String username, String firstName, String lastName) {
        Optional<String> draftId = getActiveDraftId(chatId);
        Order o = draftId.flatMap(orderRepo::findById).orElseGet(() -> {
            var now = OffsetDateTime.now();
            var n = Order.builder()
                    .id(UUID.randomUUID().toString())
                    .chatId(chatId)
                    .status(OrderStatus.DRAFT)
                    .createdAt(now)
                    .updatedAt(now)
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();
            log.debug("Create draft order {} for chat {}", n.getId(), chatId);
            return n;
        });
        if (service != null) o.setService(service);
        if (subtype != null) o.setSubtype(subtype);
        o.setUpdatedAt(OffsetDateTime.now());
        orderRepo.save(o);
        srt.opsForValue().set(draftKey(chatId), o.getId(), Duration.ofHours(2));
        return o;
    }

    public Order finalizeDraft(String orderId, String description) {
        Order o = orderRepo.findById(orderId).orElseThrow();
        o.setDescription(description);
        o.setStatus(OrderStatus.NEW);
        o.setUpdatedAt(OffsetDateTime.now());
        orderRepo.save(o);

        double score = (double) o.getCreatedAt().toEpochSecond();
        srt.opsForZSet().add(IDX_ALL, o.getId(), score);
        srt.opsForZSet().add(IDX_BY_CHAT_PREFIX + o.getChatId(), o.getId(), score);
        srt.delete(draftKey(o.getChatId()));
        log.debug("Finalize order {} for chat {}", o.getId(), o.getChatId());
        return o;
    }
    /**
     * Cancel a draft by its orderId. Removes the order entity,
     * clears the draft pointer for the chat, and cleans indices if present.
     */
    public void cancelDraft(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            return;
        }
        orderRepo.findById(orderId).ifPresent(o -> {
            // delete the order itself
            orderRepo.deleteById(orderId);
            // delete the draft pointer key
            srt.delete(draftKey(o.getChatId()));
            // remove from indices if it slipped in (should not be for DRAFT, but safe)
            srt.opsForZSet().remove(IDX_ALL, orderId);
            srt.opsForZSet().remove(IDX_BY_CHAT_PREFIX + o.getChatId(), orderId);
            log.debug("Cancel draft order {} for chat {}", orderId, o.getChatId());
        });
    }

    /**
     * Backward-compatible wrapper: cancel draft by chatId if present.
     */
    public void cancelDraft(Long chatId) {
        getActiveDraftId(chatId).ifPresent(this::cancelDraft);
        log.debug("Cancel draft for chat {}", chatId);
    }

    public Optional<Order> get(String id) { return orderRepo.findById(id); }

    public boolean updateStatus(String id, OrderStatus newStatus) {
        return orderRepo.findById(id).map(o -> {
            o.setStatus(newStatus);
            o.setUpdatedAt(OffsetDateTime.now());
            orderRepo.save(o);
            log.debug("Set status {} for order {}", newStatus, id);
            return true;
        }).orElse(false);
    }

    public List<Order> latest(int limit) {
        Set<String> ids = srt.opsForZSet().reverseRange(IDX_ALL, 0, limit - 1);
        if (ids == null || ids.isEmpty()) return List.of();
        Map<String, Order> map = new HashMap<>();
        orderRepo.findAllById(ids).forEach(o -> map.put(o.getId(), o));
        return ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Order> latestByChat(Long chatId, int limit) {
        String key = IDX_BY_CHAT_PREFIX + chatId;
        Set<String> ids = srt.opsForZSet().reverseRange(key, 0, limit - 1);
        if (ids == null || ids.isEmpty()) return List.of();
        Map<String, Order> map = new HashMap<>();
        orderRepo.findAllById(ids).forEach(o -> map.put(o.getId(), o));
        return ids.stream().map(map::get).filter(Objects::nonNull).collect(Collectors.toList());
    }
}