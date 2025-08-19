package com.example.bot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@RequiredArgsConstructor
public class BotConfig {

    @Bean
    public TelegramClient telegramClient(BotProperties props) {
        return new OkHttpTelegramClient(props.getToken());
    }
}