package com.insightflow.repository;

import com.insightflow.model.Pipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PipelineRepository extends JpaRepository<Pipeline, Long> {
    List<Pipeline> findAllByOrderByCreatedAtDesc();
    List<Pipeline> findByDataSourceId(Long dataSourceId);
}
