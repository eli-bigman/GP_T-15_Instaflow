package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.FeedbackSubmissionDto;
import com.insightflow.model.FeedbackSubmission;
import com.insightflow.service.FeedbackIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingestion")
@RequiredArgsConstructor
public class FeedbackWebhookController {

    private final FeedbackIngestionService feedbackIngestionService;

    // -------------------------------------------------------------------------
    // Pull triggers — JWT-protected (internal / admin use)
    // -------------------------------------------------------------------------

    /** Triggers today's feedback sync (same as the daily @Scheduled job). */
    @PostMapping("/feedback/sync")
    public ResponseEntity<ApiResponse<FeedbackIngestionService.FetchResult>> syncToday() {
        return ResponseEntity.ok(ApiResponse.success("Feedback sync completed", feedbackIngestionService.fetchTodaysFeedback()));
    }

    /** Triggers a feedback sync for a specific calendar date (yyyy-MM-dd). */
    @PostMapping("/feedback/sync/date/{date}")
    public ResponseEntity<ApiResponse<FeedbackIngestionService.FetchResult>> syncByDate(@PathVariable String date) {
        return ResponseEntity.ok(ApiResponse.success("Feedback sync completed", feedbackIngestionService.fetchFeedbackByDate(date)));
    }

    /** Triggers a feedback sync for a specific day of the week (e.g. "thursday"). */
    @PostMapping("/feedback/sync/day/{day}")
    public ResponseEntity<ApiResponse<FeedbackIngestionService.FetchResult>> syncByDay(@PathVariable String day) {
        return ResponseEntity.ok(ApiResponse.success("Feedback sync completed", feedbackIngestionService.fetchFeedbackByDay(day)));
    }

    /** Triggers a full sync of all feedback from the external API. */
    @PostMapping("/feedback/sync/all")
    public ResponseEntity<ApiResponse<FeedbackIngestionService.FetchResult>> syncAll() {
        return ResponseEntity.ok(ApiResponse.success("Feedback sync completed", feedbackIngestionService.fetchAllFeedback()));
    }

    // -------------------------------------------------------------------------
    // Push endpoint — public (external server pushes directly to us)
    // -------------------------------------------------------------------------

    /**
     * Receives a single feedback item pushed in real time by the ShopSmart platform.
     * Public endpoint — no authentication required.
     *
     * 400 Bad Request — missing/invalid field
     * 404 Not Found   — reference_id not found in orders staging (orphan)
     */
    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<FeedbackSubmission>> receiveFeedback(
            @RequestBody FeedbackSubmissionDto dto) {
        FeedbackSubmission saved = feedbackIngestionService.ingest(dto);
        return ResponseEntity.ok(ApiResponse.success("Feedback ingested successfully", saved));
    }
}

