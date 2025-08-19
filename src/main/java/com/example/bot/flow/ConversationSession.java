package com.example.bot.flow;

import lombok.*;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSession implements Serializable {
    private Long chatId;
    private FlowState state;
    private String service;
    private String subtype;
    private String descriptionDraft;
    private String currentOrderId;
    /** UNIX-seconds — когда сессия последний раз трогалась */
    private Long lastActivityTs;

    public static ConversationSession of(long chatId) {
        return ConversationSession.builder()
                .chatId(chatId)
                .state(FlowState.IDLE)
                .lastActivityTs(System.currentTimeMillis() / 1000)
                .build();
    }
}