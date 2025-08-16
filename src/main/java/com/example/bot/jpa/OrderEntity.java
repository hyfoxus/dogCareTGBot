package com.example.bot.jpa;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "orders",
        indexes = {
                @Index(name = "idx_orders_chat_created", columnList = "chatId,createdAt"),
                @Index(name = "idx_orders_status", columnList = "status")
        })
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {

    @Id
    @Column(length = 64)
    private String id;           // тот же String orderId, что и в Redis

    private Long chatId;

    @Column(length = 120)
    private String service;

    @Column(length = 120)
    private String subtype;

    @Column(length = 4000)
    private String description;

    @Column(length = 40)
    private String status;       // "DRAFT", "NEW", "IN_PROGRESS", "COMPLETED", "CANCELED" и т.п.

    private Instant createdAt;   // храним в UTC
    private Instant updatedAt;
}