package com.insightflow.repository;

import com.insightflow.model.Order;
import com.insightflow.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {
    boolean existsByOrderId(String orderId);
    List<Order> findByOrderStatus(OrderStatus orderStatus);
}
