package com.insightflow.model;

import com.insightflow.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    /** Masked/hashed before staging write */
    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false)
    private OrderStatus orderStatus;

    @Column(name = "delivery_region", nullable = false)
    private String deliveryRegion;

    @Column(name = "delivery_city", nullable = false)
    private String deliveryCity;

    /** Masked before staging write */
    @Column(name = "delivery_address")
    private String deliveryAddress;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "total_order_value_ghs", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalOrderValueGhs;

    /** Stored as GMT+0 after conversion from UTC */
    @Column(name = "order_timestamp_utc", nullable = false)
    private LocalDateTime orderTimestampUtc;

    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
