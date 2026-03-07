package com.insightflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.insightflow.dto.FeedbackSubmissionDto;
import com.insightflow.exception.AppException;
import com.insightflow.model.FeedbackSubmission;
import com.insightflow.model.Order;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.FeedbackCategory;
import com.insightflow.model.enums.SourceType;
import com.insightflow.repository.FeedbackSubmissionRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FeedbackIngestionService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackIngestionService.class);

    private static final int COMMENT_MAX_LENGTH = 1000;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("(\\+?\\d[\\d\\s\\-().]{7,}\\d)");

    private final FeedbackSubmissionRepository feedbackRepository;
    private final OrderRepository orderRepository;
    private final DataSourceService dataSourceService;

    @Value("${app.external.server.feedback.url}")
    private String feedbackUrl;

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
    // Scheduled pull — runs daily at 01:05 GMT+0 (after orders sync at 01:00)
    // -------------------------------------------------------------------------

    @Scheduled(cron = "0 5 1 * * *", zone = "UTC")
    public FetchResult fetchTodaysFeedback() {
        return fetchFeedbackByDate(LocalDate.now(ZoneOffset.UTC).toString());
    }

    // -------------------------------------------------------------------------
    // Public pull entry points (also manually triggerable via controller)
    // -------------------------------------------------------------------------

    /** Fetches and stages all feedback submitted on a specific calendar date (yyyy-MM-dd). */
    public FetchResult fetchFeedbackByDate(String date) {
        log.info("Syncing feedback for date: {}", date);
        return processFeedback(fetchFeedbackList(feedbackUrl + "/date/" + date));
    }

    /** Fetches and stages all feedback submitted on a given day of the week (e.g. "thursday"). */
    public FetchResult fetchFeedbackByDay(String day) {
        log.info("Syncing feedback for day: {}", day);
        return processFeedback(fetchFeedbackList(feedbackUrl + "/day/" + day));
    }

    /** Fetches and stages the full feedback list from the external API. */
    public FetchResult fetchAllFeedback() {
        log.info("Syncing all feedback");
        return processFeedback(fetchFeedbackList(feedbackUrl));
    }

    // -------------------------------------------------------------------------
    // Webhook push — single item from POST /api/v1/ingestion/feedback
    // -------------------------------------------------------------------------

    /**
     * Validates, sanitizes, and persists an incoming feedback webhook payload.
     *
     * @throws AppException (400) on validation failure
     * @throws AppException (404) if reference_id is not found in the orders staging table (orphan)
     */
    public FeedbackSubmission ingest(FeedbackSubmissionDto dto) {
        validateRequiredFields(dto);
        validateRating(dto.getFeedbackId(), dto.getRating());
        String normalizedCategory = validateAndNormalizeCategory(dto.getFeedbackId(), dto.getCategory());

        Order order = orderRepository.findById(dto.getReferenceId())
                .orElseThrow(() -> {
                    log.warn("Orphan feedback {} — order {} not found in staging",
                            dto.getFeedbackId(), dto.getReferenceId());
                    return AppException.notFound(
                            "reference_id '" + dto.getReferenceId() + "' not found in orders staging");
                });

        if (feedbackRepository.existsByFeedbackId(dto.getFeedbackId())) {
            log.info("Skipping duplicate feedback: {}", dto.getFeedbackId());
            return feedbackRepository.findById(dto.getFeedbackId()).orElseThrow();
        }

        String sanitizedComment = sanitizeComment(dto.getComment());

        FeedbackSubmission submission = FeedbackSubmission.builder()
                .feedbackId(dto.getFeedbackId())
                .orderId(dto.getReferenceId())
                .customerId(order.getCustomerId())
                .deliveryRegion(order.getDeliveryRegion())
                .rating(dto.getRating())
                .category(normalizedCategory)
                .comment(sanitizedComment)
                .submittedAt(dto.getSubmittedAt().atOffset(ZoneOffset.UTC).toLocalDateTime())
                .ingestedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        FeedbackSubmission saved = feedbackRepository.save(submission);
        log.info("Ingested feedback {} for order {}", saved.getFeedbackId(), saved.getOrderId());
        return saved;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Core batch pipeline — runs each item through ingest(), counts outcomes. */
    private FetchResult processFeedback(List<FeedbackSubmissionDto> items) {
        int fetched = items.size(), saved = 0, duplicates = 0, orphans = 0, failures = 0;

        for (FeedbackSubmissionDto dto : items) {
            if (dto.getFeedbackId() != null && feedbackRepository.existsByFeedbackId(dto.getFeedbackId())) {
                log.debug("Skipping duplicate feedback: {}", dto.getFeedbackId());
                duplicates++;
                continue;
            }
            try {
                ingest(dto);
                saved++;
            } catch (AppException e) {
                if (e.getStatus().value() == 404) orphans++;
                else failures++;
                log.warn("Skipped feedback {}: {}", dto.getFeedbackId(), e.getMessage());
            } catch (Exception e) {
                failures++;
                log.error("Unexpected error processing feedback {}: {}", dto.getFeedbackId(), e.getMessage(), e);
            }
        }

        log.info("Feedback sync complete — fetched={}, saved={}, duplicates={}, orphans={}, failures={}",
                fetched, saved, duplicates, orphans, failures);
        FetchResult result = new FetchResult(fetched, saved, duplicates, orphans, failures);
        dataSourceService.recordIngestion(
                "ShopSmart Feedback API",
                "Customer feedback submissions from the ShopSmart online channel",
                feedbackUrl,
                DataSourceType.API, SourceType.API,
                saved);
        return result;
    }

    /** Calls the given URL and flexibly deserializes the response — handles both plain arrays and wrapped objects. */
    private List<FeedbackSubmissionDto> fetchFeedbackList(String url) {
        try {
            String json = restClient.get().uri(url).retrieve().body(String.class);
            if (json == null || json.isBlank()) return List.of();

            JsonNode root = objectMapper.readTree(json);

            // Plain JSON array
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<>() {});
            }

            // Wrapped object — find the first array field (e.g. "feedback", "data", "results")
            for (JsonNode field : root) {
                if (field.isArray()) {
                    return objectMapper.convertValue(field, new TypeReference<>() {});
                }
            }

            log.warn("Unexpected response structure from [{}] — no array found", url);
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch feedback [{}]: {}", url, e.getMessage());
            return List.of();
        }
    }

    private void validateRequiredFields(FeedbackSubmissionDto dto) {
        if (isBlank(dto.getFeedbackId()))
            throw AppException.badRequest("Missing required field: feedback_id");
        if (isBlank(dto.getReferenceId()))
            throw AppException.badRequest("Missing required field: reference_id");
        if (dto.getRating() == null)
            throw AppException.badRequest("Missing required field: rating");
        if (isBlank(dto.getCategory()))
            throw AppException.badRequest("Missing required field: category");
        if (dto.getSubmittedAt() == null)
            throw AppException.badRequest("Missing required field: submitted_at");
    }

    private void validateRating(String feedbackId, Short rating) {
        if (rating < 1 || rating > 5) {
            log.warn("Rejected feedback {} — rating {} is outside range 1-5", feedbackId, rating);
            throw AppException.badRequest(
                    "Invalid rating " + rating + " for feedback " + feedbackId + " — must be between 1 and 5");
        }
    }

    private String validateAndNormalizeCategory(String feedbackId, String raw) {
        try {
            return FeedbackCategory.fromValue(raw).getValue();
        } catch (IllegalArgumentException e) {
            log.warn("Rejected feedback {} — unknown category '{}'", feedbackId, raw);
            throw AppException.badRequest("Invalid category '" + raw + "' for feedback " + feedbackId);
        }
    }

    private String sanitizeComment(String comment) {
        if (comment == null) return null;
        String sanitized = EMAIL_PATTERN.matcher(comment).replaceAll("[REDACTED]");
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[REDACTED]");
        if (sanitized.length() > COMMENT_MAX_LENGTH) {
            log.debug("Truncating comment from {} to {} characters", sanitized.length(), COMMENT_MAX_LENGTH);
            sanitized = sanitized.substring(0, COMMENT_MAX_LENGTH);
        }
        return sanitized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // -------------------------------------------------------------------------
    // Fetch result — returned by batch pull methods
    // -------------------------------------------------------------------------

    @Getter
    @AllArgsConstructor
    public static class FetchResult {
        private final int fetched;
        private final int saved;
        private final int duplicatesSkipped;
        private final int orphansDropped;
        private final int failures;
    }
}

