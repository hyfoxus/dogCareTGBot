package com.example.bot.orders;

import com.example.bot.jpa.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByChatId(Long chatId);
}