package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_submissions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackSubmission {

    @Id
    @Column(name = "feedback_id", nullable = false, length = 20)
    private String feedbackId;

    /**
     * FK to orders.order_id. Called "reference_id" in the external API payload
     * but stored as order_id to match the DB schema.
     */
    @Column(name = "order_id", nullable = false, length = 20)
    private String orderId;

    /** Nullable — may be absent in webhook payload */
    @Column(name = "customer_id", length = 64)
    private String customerId;

    @Column(name = "delivery_region", nullable = false, length = 50)
    private String deliveryRegion;

    /** Nullable — not always present in webhook payload */
    @Column(name = "sku", length = 20)
    private String sku;

    @Column(name = "rating", nullable = false)
    private Short rating;

    /**
     * Stored as the DB-compatible display string (e.g. "Delivery Speed").
     * Use {@link com.insightflow.model.enums.FeedbackCategory} for validation.
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** Truncated to 1000 characters before persisting; PII-scanned for emails/phones */
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "ingested_at", nullable = false)
    private LocalDateTime ingestedAt;
}
