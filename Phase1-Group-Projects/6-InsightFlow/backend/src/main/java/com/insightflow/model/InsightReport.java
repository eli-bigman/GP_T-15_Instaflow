package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "insight_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InsightReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String summary;
    @Column(columnDefinition = "TEXT") private String dataJson;
    @ManyToOne @JoinColumn(name = "pipeline_id") private Pipeline pipeline;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
