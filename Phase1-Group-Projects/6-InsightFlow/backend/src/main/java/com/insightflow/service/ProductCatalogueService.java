package com.insightflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.insightflow.dto.ProductCatalogueDto;
import com.insightflow.exception.AppException;
import com.insightflow.model.ProductCatalogue;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.SourceType;
import com.insightflow.repository.ProductCatalogueRepository;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCatalogueService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogueService.class);

    private final ProductCatalogueRepository productRepository;
    private final DataSourceService dataSourceService;

    @Value("${app.external.server.products.url}")
    private String productsUrl;

    private org.springframework.web.client.RestClient restClient;
    private ObjectMapper objectMapper;

    @PostConstruct
    private void init() {
        restClient = org.springframework.web.client.RestClient.builder().build();
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // -------------------------------------------------------------------------
    // Scheduled pull — runs daily at 01:10 GMT+0 (after orders at 01:00 and feedback at 01:05)
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 10 1 * * *", zone = "UTC")
    public SyncResult fetchTodaysProducts() {
        return fetchProductsByDate(LocalDate.now(ZoneOffset.UTC).toString());
    }

    // -------------------------------------------------------------------------
    // Public pull entry points (also manually triggerable via controller)
    // -------------------------------------------------------------------------

    /** Fetches and upserts all products added/updated on a specific calendar date (yyyy-MM-dd). */
    public SyncResult fetchProductsByDate(String date) {
        log.info("Syncing products for date: {}", date);
        return processProducts(fetchProductList(productsUrl + "/date/" + date));
    }

    /** Fetches and upserts all products added/updated on a given day of the week (e.g. "thursday"). */
    public SyncResult fetchProductsByDay(String day) {
        log.info("Syncing products for day: {}", day);
        return processProducts(fetchProductList(productsUrl + "/day/" + day));
    }

    /** Fetches and upserts the full product catalogue from the external API. */
    public SyncResult fetchAllProducts() {
        log.info("Syncing all products");
        return processProducts(fetchProductList(productsUrl));
    }

    // -------------------------------------------------------------------------
    // Webhook push — single product from POST /api/v1/ingestion/products
    // -------------------------------------------------------------------------

    /**
     * Validates and upserts an incoming product webhook payload.
     * Inserts new SKUs; updates existing ones to keep the catalogue current.
     *
     * @throws AppException (400) on validation failure
     */
    public ProductCatalogue ingest(ProductCatalogueDto dto) {
        validateRequiredFields(dto);

        // Upsert — products change over time (name, is_active, pricing)
        ProductCatalogue product = productRepository.findBySku(dto.getSku())
                .orElse(ProductCatalogue.builder().sku(dto.getSku()).build());

        product.setProductName(dto.getProductName());
        product.setCategory(dto.getCategory());
        product.setBrand(dto.getBrand());
        product.setUnitOfMeasure(dto.getUnitOfMeasure());
        product.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);

        ProductCatalogue saved = productRepository.save(product);
        log.info("{} product: sku={}, name={}",
                product.getProductId() == null ? "Inserted" : "Updated",
                saved.getSku(), saved.getProductName());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Core batch pipeline — validates and upserts each product. */
    private SyncResult processProducts(List<ProductCatalogueDto> items) {
        int fetched = items.size(), inserted = 0, updated = 0, failures = 0;

        for (ProductCatalogueDto dto : items) {
            try {
                boolean existed = productRepository.existsBySku(dto.getSku());
                ingest(dto);
                if (existed) updated++; else inserted++;
            } catch (AppException e) {
                failures++;
                log.warn("Skipped product sku={}: {}", dto.getSku(), e.getMessage());
            } catch (Exception e) {
                failures++;
                log.error("Unexpected error processing product sku={}: {}", dto.getSku(), e.getMessage(), e);
            }
        }

        log.info("Products sync complete — fetched={}, inserted={}, updated={}, failures={}",
                fetched, inserted, updated, failures);
        SyncResult result = new SyncResult(fetched, inserted, updated, failures);
        dataSourceService.recordIngestion(
                "ShopSmart Products API",
                "Product catalogue from the ShopSmart online channel",
                productsUrl,
                DataSourceType.API, SourceType.API,
                inserted + updated);
        return result;
    }

    /** Calls the given URL and flexibly deserializes the response — handles both plain arrays and wrapped objects. */
    private List<ProductCatalogueDto> fetchProductList(String url) {
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return List.of();

            JsonNode root = objectMapper.readTree(json);

            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<>() {});
            }

            // Wrapped object — find the first array field (e.g. "products", "data", "results")
            for (JsonNode field : root) {
                if (field.isArray()) {
                    return objectMapper.convertValue(field, new TypeReference<>() {});
                }
            }

            log.warn("Unexpected response structure from [{}] — no array found", url);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch products [{}]: {}", url, e.getMessage());
            return List.of();
        }
    }

    private void validateRequiredFields(ProductCatalogueDto dto) {
        if (isBlank(dto.getSku()))
            throw AppException.badRequest("Missing required field: sku");
        if (isBlank(dto.getProductName()))
            throw AppException.badRequest("Missing required field: product_name");
        if (isBlank(dto.getCategory()))
            throw AppException.badRequest("Missing required field: category");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // -------------------------------------------------------------------------
    // Sync result — returned by all batch pull methods
    // -------------------------------------------------------------------------

    @Getter
    @AllArgsConstructor
    public static class SyncResult {
        private final int fetched;
        private final int inserted;
        private final int updated;
        private final int failures;
    }
}
