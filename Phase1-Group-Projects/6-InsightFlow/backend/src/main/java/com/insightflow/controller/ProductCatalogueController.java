package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.ProductCatalogueDto;
import com.insightflow.model.ProductCatalogue;
import com.insightflow.service.ProductCatalogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Product Catalogue", description = "Sync product catalogue from ShopSmart external API")
public class ProductCatalogueController {

    private final ProductCatalogueService productService;

    // -------------------------------------------------------------------------
    // Pull triggers — manually trigger a sync from the external API
    // -------------------------------------------------------------------------

    @PostMapping("/api/v1/integration/products/sync/all")
    @Operation(summary = "Sync full product catalogue from external API")
    public ResponseEntity<ApiResponse<ProductCatalogueService.SyncResult>> syncAll() {
        ProductCatalogueService.SyncResult result = productService.fetchAllProducts();
        return ResponseEntity.ok(ApiResponse.success("Product catalogue sync completed", result));
    }

    @PostMapping("/api/v1/integration/products/sync/date/{date}")
    @Operation(summary = "Sync products updated on a specific date (yyyy-MM-dd)")
    public ResponseEntity<ApiResponse<ProductCatalogueService.SyncResult>> syncByDate(
            @PathVariable String date) {
        ProductCatalogueService.SyncResult result = productService.fetchProductsByDate(date);
        return ResponseEntity.ok(ApiResponse.success("Product sync by date completed", result));
    }

    @PostMapping("/api/v1/integration/products/sync/day/{day}")
    @Operation(summary = "Sync products updated on a given day of the week (e.g. thursday)")
    public ResponseEntity<ApiResponse<ProductCatalogueService.SyncResult>> syncByDay(
            @PathVariable String day) {
        ProductCatalogueService.SyncResult result = productService.fetchProductsByDay(day);
        return ResponseEntity.ok(ApiResponse.success("Product sync by day completed", result));
    }

    // -------------------------------------------------------------------------
    // Push endpoint — external server sends a product directly to us
    // -------------------------------------------------------------------------

    @PostMapping("/api/v1/ingestion/products")
    @Operation(summary = "Receive a single product push from the external ShopSmart server")
    public ResponseEntity<ApiResponse<ProductCatalogue>> receiveProduct(
            @RequestBody ProductCatalogueDto dto) {
        ProductCatalogue saved = productService.ingest(dto);
        return ResponseEntity.ok(ApiResponse.success("Product ingested successfully", saved));
    }
}
