package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.OrderDto;
import com.insightflow.service.ApiFeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderFeedController {

    private final ApiFeedService apiFeedService;

    // -------------------------------------------------------------------------
    // Pull triggers — JWT-protected (internal / admin use)
    // -------------------------------------------------------------------------

    /** Triggers today's order sync (same as the daily @Scheduled job). */
    @PostMapping("/api/v1/integration/orders/sync")
    public ResponseEntity<ApiResponse<ApiFeedService.SyncResult>> syncToday() {
        return ResponseEntity.ok(ApiResponse.success("Order sync completed", apiFeedService.fetchTodaysOrders()));
    }

    /** Triggers a sync for a specific calendar date (yyyy-MM-dd). */
    @PostMapping("/api/v1/integration/orders/sync/date/{date}")
    public ResponseEntity<ApiResponse<ApiFeedService.SyncResult>> syncByDate(@PathVariable String date) {
        return ResponseEntity.ok(ApiResponse.success("Order sync completed", apiFeedService.fetchOrdersByDate(date)));
    }

    /** Triggers a sync for a specific day of the week (e.g. "tuesday"). */
    @PostMapping("/api/v1/integration/orders/sync/day/{day}")
    public ResponseEntity<ApiResponse<ApiFeedService.SyncResult>> syncByDay(@PathVariable String day) {
        return ResponseEntity.ok(ApiResponse.success("Order sync completed", apiFeedService.fetchOrdersByDay(day)));
    }

    /** Triggers a full sync of all orders from the external API. */
    @PostMapping("/api/v1/integration/orders/sync/all")
    public ResponseEntity<ApiResponse<ApiFeedService.SyncResult>> syncAll() {
        return ResponseEntity.ok(ApiResponse.success("Order sync completed", apiFeedService.fetchAllOrders()));
    }

    // -------------------------------------------------------------------------
    // Push endpoint — public (external server pushes directly to us)
    // -------------------------------------------------------------------------

    /** Receives a single order pushed in real time by the external ShopSmart server. */
    @PostMapping("/api/feed/orders")
    public ResponseEntity<ApiResponse<Void>> ingestOrder(@RequestBody OrderDto orderDto) {
        apiFeedService.ingestSingleOrder(orderDto);
        return ResponseEntity.ok(ApiResponse.success("Order ingested successfully", null));
    }

    // -------------------------------------------------------------------------
    // Stub
    // -------------------------------------------------------------------------

    /** Inventory feed stub — pending hand-off with Dev C (Gideon). */
    @PostMapping("/api/feed/inventory")
    public ResponseEntity<ApiResponse<Void>> inventoryStub() {
        return ResponseEntity.status(501)
                .body(ApiResponse.error("Inventory feed not yet implemented — pending Dev C hand-off"));
    }
}
