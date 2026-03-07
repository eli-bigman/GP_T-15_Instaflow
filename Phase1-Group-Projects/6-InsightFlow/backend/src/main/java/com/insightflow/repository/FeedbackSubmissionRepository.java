package com.insightflow.repository;

import com.insightflow.model.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, String> {
    boolean existsByFeedbackId(String feedbackId);
    List<FeedbackSubmission> findByOrderId(String orderId);
}
