package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.SourceType;
import java.time.LocalDateTime;

@Entity @Table(name = "data_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DataSourceType type;
    @Enumerated(EnumType.STRING) private SourceType sourceType;
    private String connectionUrl;
    private String filePath;
    private Boolean isActive;
    private Integer recordCount;
    private LocalDateTime lastIngestion;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
