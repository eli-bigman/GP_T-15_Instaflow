package com.insightflow.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PipelineResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private String dataSourceName;
    private String createdByName;
    private Long recordsProcessed;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
