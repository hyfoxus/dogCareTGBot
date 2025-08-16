package com.example.bot.jpa;

import com.example.bot.orders.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderPersistence {

    private final OrderJpaRepository repo;

    /** Сохранить/обновить заказ в H2 из доменной модели (Redis-модели). */
    @Transactional
    public void saveFromModel(Order o) {
        if (o == null || o.getId() == null) return;

        OrderEntity e = OrderEntity.builder()
                .id(o.getId())
                .chatId(o.getChatId())
                .service(o.getService())
                .subtype(o.getSubtype())
                .description(o.getDescription())
                .status(o.getStatus().toString()) // строковый статус
                .createdAt(toInstantSafe(o.getCreatedAt()))
                .updatedAt(toInstantSafe(o.getUpdatedAt()))
                .build();

        repo.save(e);
    }

    /** Удалить запись из H2 (например, при отмене черновика). */
    @Transactional
    public void deleteById(String orderId) {
        if (orderId == null || orderId.isBlank()) return;
        repo.deleteById(orderId);
    }

    private static Instant toInstantSafe(java.time.OffsetDateTime odt) {
        return odt != null ? odt.toInstant() : null;
    }
}