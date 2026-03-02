package com.insightflow.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DataSourceResponse {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String connectionUrl;
    private String createdByName;
    private LocalDateTime createdAt;
}
