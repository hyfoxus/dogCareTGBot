package com.example.bot.jpa;

import com.example.bot.orders.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEntity {

    @Id
    private String id;

    private Long chatId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String service;
    private String subtype;

    @Lob
    private String description;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // === mapping helpers ===
    public static OrderEntity fromModel(com.example.bot.orders.Order m) {
        if (m == null) return null;
        return OrderEntity.builder()
                .id(m.getId())
                .chatId(m.getChatId())
                .status(m.getStatus())
                .service(m.getService())
                .subtype(m.getSubtype())
                .description(m.getDescription())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }

    public com.example.bot.orders.Order toModel() {
        return com.example.bot.orders.Order.builder()
                .id(id)
                .chatId(chatId)
                .status(status)
                .service(service)
                .subtype(subtype)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}