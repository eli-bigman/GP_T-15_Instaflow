package com.insightflow.repository;

import com.insightflow.model.InsightReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InsightReportRepository extends JpaRepository<InsightReport, Long> {
    List<InsightReport> findByPipelineIdOrderByCreatedAtDesc(Long pipelineId);
    List<InsightReport> findAllByOrderByCreatedAtDesc();
}
