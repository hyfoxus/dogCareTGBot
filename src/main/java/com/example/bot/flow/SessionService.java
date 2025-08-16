package com.example.bot.flow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final ConversationSessionRepository repo;

    public ConversationSession get(long chatId) {
        return repo.findById(chatId).orElseGet(() -> {
            var s = ConversationSession.of(chatId);
            log.debug("Create new session for chat {}", chatId);
            return repo.save(s);
        });
    }

    public void save(ConversationSession s) {
        s.setLastActivityTs(System.currentTimeMillis() / 1000);
        repo.save(s);
    }

    public void reset(long chatId) {
        repo.deleteById(chatId);
    }
}