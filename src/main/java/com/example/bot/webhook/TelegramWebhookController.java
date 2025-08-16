package com.example.bot.webhook;

import com.example.bot.config.WebhookProperties;
import com.example.bot.core.UpdateRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Objects;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("${telegram.webhook.path:/webhook/telegram}")
public class TelegramWebhookController {

    private final UpdateRouter router;
    private final WebhookProperties props;

    @PostMapping
    public ResponseEntity<String> onUpdate(@RequestBody Update update,
                                           @RequestHeader(value = "X-Telegram-Bot-Api-Secret-Token", required = false) String token) {
        String expected = props.getSecretToken();
        if (expected != null && !expected.isBlank() && !Objects.equals(expected, token)) {
            log.warn("Reject webhook: bad secret");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("bad secret");
        }
        router.consume(update);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/health")
    public String health() { return "ok"; }
}