package com.insightflow.model;

import com.insightflow.model.enums.PipelineStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "pipelines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pipeline {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) private PipelineStatus status = PipelineStatus.PENDING;
    @ManyToOne @JoinColumn(name = "data_source_id") private DataSource dataSource;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;
    private Long recordsProcessed;
    private String errorMessage;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
