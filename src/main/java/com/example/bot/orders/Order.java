package com.example.bot.orders;

import lombok.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order implements Serializable {
    private String id;
    private Long chatId;
    private OrderStatus status;
    private String service;
    private String subtype;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}