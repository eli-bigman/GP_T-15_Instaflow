package com.insightflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Maps the raw JSON order payload returned by the ShopSmart external API. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderDto {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("order_status")
    private String orderStatus;

    @JsonProperty("delivery_region")
    private String deliveryRegion;

    @JsonProperty("delivery_city")
    private String deliveryCity;

    @JsonProperty("delivery_address")
    private String deliveryAddress;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("total_order_value_ghs")
    private BigDecimal totalOrderValueGhs;

    /** UTC timestamp string from the external API — converted to GMT+0 before persisting */
    @JsonProperty("order_timestamp_utc")
    private Instant orderTimestampUtc;

    @JsonProperty("order_items")
    private List<OrderItemDto> items;
}
