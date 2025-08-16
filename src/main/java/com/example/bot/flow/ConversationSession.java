package com.example.bot.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@RedisHash("session")
public class ConversationSession {
    @Id
    private Long chatId;

    @Builder.Default
    private FlowState state = FlowState.IDLE;

    private String service;
    private String subtype;
    private String descriptionDraft;
    private String currentOrderId;

    private long lastActivityTs;

    @TimeToLive
    @Builder.Default
    private Long ttlSec = 3600L;

    public static ConversationSession of(Long chatId) {
        return ConversationSession.builder()
                .chatId(chatId)
                .lastActivityTs(System.currentTimeMillis() / 1000)
                .build();
    }
}