package com.example.bot.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findTop50ByOrderByCreatedAtDesc();
    List<OrderEntity> findTop50ByChatIdOrderByCreatedAtDesc(Long chatId);
}