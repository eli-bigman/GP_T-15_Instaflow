package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;
import com.insightflow.model.enums.JobStatus;
import java.time.LocalDateTime;

@Entity @Table(name = "ingestion_jobs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IngestionJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne @JoinColumn(name = "data_source_id") private DataSource dataSource;
    private String fileName;
    @Enumerated(EnumType.STRING) private JobStatus status;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    @Column(columnDefinition = "TEXT") private String validationErrors;
    @Column(name = "started_at") private LocalDateTime startedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
}
