package com.insightflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.insightflow.dto.OrderDto;
import com.insightflow.exception.AppException;
import com.insightflow.model.Order;
import com.insightflow.model.OrderItem;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.SourceType;
import com.insightflow.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiFeedService {

    private static final Logger log = LoggerFactory.getLogger(ApiFeedService.class);

    private final OrderRepository orderRepository;
    private final StandardizationService standardizationService;
    private final DataSourceService dataSourceService;

    @Value("${app.external.server.orders.url}")
    private String ordersUrl;

    private RestClient restClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    private void init() {
        restClient = RestClient.builder().build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // -------------------------------------------------------------------------
    // Public sync entry points
    // -------------------------------------------------------------------------

    /** Runs daily at 01:00 GMT+0 — fetches all orders placed on the current UTC date. */
    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    public SyncResult fetchTodaysOrders() {
        return fetchOrdersByDate(LocalDate.now(ZoneOffset.UTC).toString());
    }

    /** Fetches and stages all orders for a specific calendar date (yyyy-MM-dd). */
    public SyncResult fetchOrdersByDate(String date) {
        log.info("Syncing orders for date: {}", date);
        return processOrders(fetchOrderList(ordersUrl + "/date/" + date));
    }

    /** Fetches and stages all orders placed on a given day of the week (e.g. "tuesday"). */
    public SyncResult fetchOrdersByDay(String day) {
        log.info("Syncing orders for day: {}", day);
        return processOrders(fetchOrderList(ordersUrl + "/day/" + day));
    }

    /** Fetches and stages the full order list from the external API. */
    public SyncResult fetchAllOrders() {
        log.info("Syncing all orders");
        return processOrders(fetchOrderList(ordersUrl));
    }

    /**
     * Push model — stages a single order received directly via POST /api/feed/orders.
     * Applies the same dedup, validation, masking, and filter pipeline as the batch sync.
     */
    public void ingestSingleOrder(OrderDto dto) {
        if (orderRepository.existsByOrderId(dto.getOrderId())) {
            log.info("Ignoring duplicate push for order: {}", dto.getOrderId());
            return;
        }
        Order order = standardizationService.standardize(dto);
        validateTotal(order);
        order.setCustomerId(maskPii(order.getCustomerId()));
        if (order.getDeliveryAddress() != null) {
            order.setDeliveryAddress(maskPii(order.getDeliveryAddress()));
        }
        order.setIngestedAt(LocalDateTime.now(ZoneOffset.UTC));
        orderRepository.save(order);
        log.info("Push-ingested order: {}", order.getOrderId());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Core processing pipeline — dedup, validate, mask, filter, persist. */
    private SyncResult processOrders(List<OrderDto> rawOrders) {
        int fetched = rawOrders.size();
        int duplicates = 0, mismatches = 0, failures = 0, staged = 0, itemsStaged = 0;

        for (OrderDto dto : rawOrders) {
            if (orderRepository.existsByOrderId(dto.getOrderId())) {
                log.debug("Skipping duplicate order: {}", dto.getOrderId());
                duplicates++;
                continue;
            }
            try {
                Order order = standardizationService.standardize(dto);
                if (!validateTotal(order)) mismatches++;
                order.setCustomerId(maskPii(order.getCustomerId()));
                if (order.getDeliveryAddress() != null)
                    order.setDeliveryAddress(maskPii(order.getDeliveryAddress()));
                order.setIngestedAt(LocalDateTime.now(ZoneOffset.UTC));
                orderRepository.save(order);
                staged++;
                itemsStaged += order.getItems().size();
            } catch (AppException e) {
                failures++;
                log.error("Validation failure for order {}: {}", dto.getOrderId(), e.getMessage());
            } catch (Exception e) {
                failures++;
                log.error("Unexpected error processing order {}: {}", dto.getOrderId(), e.getMessage(), e);
            }
        }

        log.info("Orders sync complete — fetched={}, staged={}, duplicatesSkipped={}, mismatches={}, failures={}",
                fetched, staged, duplicates, mismatches, failures);
        SyncResult result = new SyncResult(fetched, staged, duplicates, mismatches, failures);
        dataSourceService.recordIngestion(
                "ShopSmart Orders API",
                "E-commerce orders from the ShopSmart online channel",
                ordersUrl,
                DataSourceType.API, SourceType.API,
                staged);
        dataSourceService.recordIngestion(
                "ShopSmart Order Items",
                "Order line items embedded in ShopSmart order JSON payloads",
                ordersUrl,
                DataSourceType.JSON, SourceType.JSON,
                itemsStaged);
        return result;
    }

    /** Calls the given URL and flexibly deserializes the response — handles both plain arrays and wrapped objects. */
    private List<OrderDto> fetchOrderList(String url) {
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return List.of();

            JsonNode root = objectMapper.readTree(json);

            // Plain JSON array
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<>() {});
            }

            // Wrapped object — find the first array field (e.g. "orders", "data", "results")
            for (JsonNode field : root) {
                if (field.isArray()) {
                    return objectMapper.convertValue(field, new TypeReference<>() {});
                }
            }

            log.warn("Unexpected response structure from [{}] — no array found", url);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch orders [{}]: {}", url, e.getMessage());
            return List.of();
        }
    }

    /**
     * Validates that SUM(order_items.line_total_ghs) equals order.total_order_value_ghs.
     * Logs a warning on mismatch; returns false so the caller can increment the mismatch counter.
     */
    private boolean validateTotal(Order order) {
        BigDecimal itemsSum = order.getItems().stream()
                .map(OrderItem::getLineTotalGhs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean matches = itemsSum.compareTo(order.getTotalOrderValueGhs()) == 0;
        if (!matches) {
            log.warn("Total mismatch for order {} — declared={}, computed={}",
                    order.getOrderId(), order.getTotalOrderValueGhs(), itemsSum);
        }
        return matches;
    }

    /**
     * One-way SHA-256 hash used to mask PII fields (customer_id, delivery_address)
     * before writing to the staging DB. Deterministic, so ETL joins on masked values are stable.
     */
    private String maskPii(String value) {
        if (value == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always present in the JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // -------------------------------------------------------------------------
    // Sync result record — returned by fetchTodaysOrders() and exposed to controller
    // -------------------------------------------------------------------------

    @Getter
    @AllArgsConstructor
    public static class SyncResult {
        private final int fetched;
        private final int staged;
        private final int duplicatesSkipped;
        private final int mismatches;
        private final int failures;
    }
}
