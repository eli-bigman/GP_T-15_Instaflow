package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {

    /** Generated as order_id + sequential suffix, e.g. "WEB-99021-001" */
    @Id
    @Column(name = "order_item_id", nullable = false, unique = true, length = 30)
    private String orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_ghs", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceGhs;

    /** quantity * unit_price_ghs — validated against order total */
    @Column(name = "line_total_ghs", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotalGhs;
}
