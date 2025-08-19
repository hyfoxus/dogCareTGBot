package com.example.bot.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory хранение сессий диалога. Без БД/Redis.
 * TTL по неактивности: 30 минут (можно поменять).
 */
@Slf4j
@Service
public class SessionService {

    // chatId -> session
    private final ConcurrentMap<Long, ConversationSession> sessions = new ConcurrentHashMap<>();

    private final Duration ttl = Duration.ofMinutes(30);

    /** Получить сессию (создать новую при отсутствии/истёкшем TTL). */
    public ConversationSession get(long chatId) {
        long nowSec = System.currentTimeMillis() / 1000;
        return sessions.compute(chatId, (k, existing) -> {
            if (existing == null || isExpired(existing, nowSec)) {
                ConversationSession s = ConversationSession.of(chatId);
                s.setLastActivityTs(nowSec);
                log.debug("Create new session for chat {}", chatId);
                return s;
            }
            existing.setLastActivityTs(nowSec);
            return existing;
        });
    }

    /** Явно сохранить изменения сессии. */
    public void save(ConversationSession s) {
        if (s == null) return;
        s.setLastActivityTs(System.currentTimeMillis() / 1000);
        sessions.put(s.getChatId(), s);
    }

    /** Сбросить сессию. */
    public void reset(long chatId) {
        sessions.remove(chatId);
    }

    /** Найти сессию без создания. */
    public Optional<ConversationSession> find(long chatId) {
        ConversationSession s = sessions.get(chatId);
        if (s == null) return Optional.empty();
        long nowSec = System.currentTimeMillis() / 1000;
        if (isExpired(s, nowSec)) {
            sessions.remove(chatId);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    private boolean isExpired(ConversationSession s, long nowSec) {
        long last = Optional.ofNullable(s.getLastActivityTs()).orElse(0L);
        return (nowSec - last) > ttl.toSeconds();
    }

    /** Периодическая чистка устаревших сессий. */
    @Scheduled(fixedDelay = 300_000) // каждые 5 минут
    public void cleanup() {
        long nowSec = System.currentTimeMillis() / 1000;
        sessions.entrySet().removeIf(e -> isExpired(e.getValue(), nowSec));
    }
}