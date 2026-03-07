package com.insightflow.service;

import com.insightflow.dto.OrderDto;
import com.insightflow.dto.OrderItemDto;
import com.insightflow.exception.AppException;
import com.insightflow.model.Order;
import com.insightflow.model.OrderItem;
import com.insightflow.model.enums.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class StandardizationService {

    private static final Logger log = LoggerFactory.getLogger(StandardizationService.class);

    private static final Set<String> VALID_REGIONS = Set.of(
            "Greater Accra", "Ashanti", "Western", "Central"
    );

    private static final Set<String> VALID_PAYMENT_METHODS = Set.of(
            "MOMO", "CARD", "COD"
    );

    /**
     * Validates and maps an {@link OrderDto} from the external API to an {@link Order} entity.
     * Does NOT set {@code ingested_at} or apply PII masking — those are handled by
     * {@link ApiFeedService} after this call.
     *
     * @throws AppException (400) if delivery_region or payment_method is invalid
     */
    public Order standardize(OrderDto dto) {
        validateRegion(dto.getOrderId(), dto.getDeliveryRegion());
        validatePaymentMethod(dto.getOrderId(), dto.getPaymentMethod());

        Order order = Order.builder()
                .orderId(dto.getOrderId())
                .customerId(dto.getCustomerId())
                .orderStatus(parseOrderStatus(dto.getOrderId(), dto.getOrderStatus()))
                .deliveryRegion(dto.getDeliveryRegion())
                .deliveryCity(dto.getDeliveryCity())
                .deliveryAddress(dto.getDeliveryAddress())
                .paymentMethod(dto.getPaymentMethod())
                .totalOrderValueGhs(dto.getTotalOrderValueGhs())
                .orderTimestampUtc(convertToGmt0(dto))
                .build();

        order.setItems(mapItems(dto.getOrderId(), dto.getItems(), order));
        return order;
    }

    private void validateRegion(String orderId, String region) {
        if (region == null || !VALID_REGIONS.contains(region)) {
            throw AppException.badRequest(
                    String.format("Order %s has invalid delivery_region: '%s'", orderId, region));
        }
    }

    private void validatePaymentMethod(String orderId, String method) {
        if (method == null || !VALID_PAYMENT_METHODS.contains(method)) {
            throw AppException.badRequest(
                    String.format("Order %s has invalid payment_method: '%s'", orderId, method));
        }
    }

    private OrderStatus parseOrderStatus(String orderId, String raw) {
        try {
            return OrderStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest(
                    String.format("Order %s has unknown order_status: '%s'", orderId, raw));
        }
    }

    /** Converts the UTC Instant from the DTO to a LocalDateTime at GMT+0. */
    private LocalDateTime convertToGmt0(OrderDto dto) {
        return dto.getOrderTimestampUtc().atOffset(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * Flattens the nested items list into {@link OrderItem} rows.
     * order_item_id format: "{orderId}-{index}" e.g. "WEB-99021-001"
     * line_total_ghs is computed as quantity * unit_price_ghs.
     */
    private List<OrderItem> mapItems(String orderId, List<OrderItemDto> rawItems, Order order) {
        List<OrderItem> items = new ArrayList<>();
        if (rawItems == null || rawItems.isEmpty()) {
            log.warn("Order {} arrived with no items", orderId);
            return items;
        }
        for (int i = 0; i < rawItems.size(); i++) {
            OrderItemDto raw = rawItems.get(i);
            // Use API-provided order_item_id if present, otherwise generate one
            String itemId = (raw.getOrderItemId() != null && !raw.getOrderItemId().isBlank())
                    ? raw.getOrderItemId()
                    : String.format("%s-%03d", orderId, i + 1);
            // Use API-provided line_total_ghs if present, otherwise compute it
            BigDecimal lineTotal = (raw.getLineTotalGhs() != null)
                    ? raw.getLineTotalGhs()
                    : raw.getUnitPriceGhs().multiply(BigDecimal.valueOf(raw.getQuantity()));
            items.add(OrderItem.builder()
                    .orderItemId(itemId)
                    .order(order)
                    .sku(raw.getSku())
                    .quantity(raw.getQuantity())
                    .unitPriceGhs(raw.getUnitPriceGhs())
                    .lineTotalGhs(lineTotal)
                    .build());
        }
        return items;
    }
}
