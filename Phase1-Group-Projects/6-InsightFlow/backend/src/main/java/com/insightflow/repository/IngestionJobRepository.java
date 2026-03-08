package com.insightflow.repository;

import com.insightflow.model.IngestionJob;
import com.insightflow.model.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, Long> {
    List<IngestionJob> findByDataSourceAndStartedAtAfterOrderByStartedAtDesc(DataSource dataSource, LocalDateTime startedAt);
}
