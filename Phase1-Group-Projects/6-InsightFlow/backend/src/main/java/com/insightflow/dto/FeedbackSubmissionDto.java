package com.insightflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

/** Maps the raw JSON payload from the ShopSmart feedback webhook. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FeedbackSubmissionDto {

    @JsonProperty("feedback_id")
    private String feedbackId;

    /** External API field name — maps to order_id in the DB */
    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("rating")
    private Short rating;

    @JsonProperty("category")
    private String category;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("submitted_at")
    private Instant submittedAt;
}
