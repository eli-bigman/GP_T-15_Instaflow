package com.insightflow.model;

import com.insightflow.model.enums.DataSourceType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "data_sources")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataSource {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DataSourceType type;
    private String connectionUrl;
    private String filePath;
    @ManyToOne @JoinColumn(name = "created_by") private User createdBy;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
}
