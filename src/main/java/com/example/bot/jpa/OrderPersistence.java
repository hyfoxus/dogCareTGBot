package com.example.bot.jpa;

import com.example.bot.orders.Order;
import com.example.bot.orders.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderPersistence {

    private final OrderRepository repo;

    public void saveFromModel(Order m) {
        repo.save(OrderEntity.fromModel(m));
    }

    public void deleteById(String id) {
        repo.deleteById(id);
    }

    public Optional<Order> findById(String id) {
        return repo.findById(id).map(OrderEntity::toModel);
    }

    public List<Order> findByChatId(Long chatId) {
        return repo.findByChatId(chatId).stream().map(OrderEntity::toModel).toList();
    }
}