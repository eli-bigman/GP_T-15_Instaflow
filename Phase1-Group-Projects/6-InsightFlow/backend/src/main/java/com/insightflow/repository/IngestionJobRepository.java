package com.insightflow.repository;

import com.insightflow.model.IngestionJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
}
