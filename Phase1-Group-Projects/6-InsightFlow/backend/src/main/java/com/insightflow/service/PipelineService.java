package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.model.*;
import com.insightflow.model.enums.PipelineStatus;
import com.insightflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class PipelineService {
    private final PipelineRepository pipelineRepository;
    private final DataSourceRepository dataSourceRepository;

    public List<PipelineResponse> getAll() {
        return pipelineRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    public PipelineResponse create(PipelineRequest request, User user) {
        DataSource ds = dataSourceRepository.findById(request.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("Data source not found"));
        Pipeline pipeline = Pipeline.builder()
                .name(request.getName())
                .description(request.getDescription())
                .dataSource(ds)
                .createdBy(user)
                .status(PipelineStatus.PENDING)
                .build();
        return toResponse(pipelineRepository.save(pipeline));
    }

    public PipelineResponse runPipeline(Long id) {
        Pipeline pipeline = pipelineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pipeline not found"));
        pipeline.setStatus(PipelineStatus.RUNNING);
        pipeline.setStartedAt(LocalDateTime.now());
        pipelineRepository.save(pipeline);

        // TODO: Actual pipeline execution logic
        // For now, simulate completion
        pipeline.setStatus(PipelineStatus.COMPLETED);
        pipeline.setCompletedAt(LocalDateTime.now());
        pipeline.setRecordsProcessed(0L);
        return toResponse(pipelineRepository.save(pipeline));
    }

    // TODO: Implement schedule pipeline
    // TODO: Implement pipeline history

    private PipelineResponse toResponse(Pipeline p) {
        return PipelineResponse.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .status(p.getStatus().name())
                .dataSourceName(p.getDataSource() != null ? p.getDataSource().getName() : null)
                .createdByName(p.getCreatedBy() != null ? p.getCreatedBy().getName() : null)
                .recordsProcessed(p.getRecordsProcessed())
                .errorMessage(p.getErrorMessage())
                .startedAt(p.getStartedAt()).completedAt(p.getCompletedAt())
                .createdAt(p.getCreatedAt()).build();
    }
}
