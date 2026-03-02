package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DataSourceRequest {
    @NotBlank private String name;
    private String description;
    @NotBlank private String type;
    private String connectionUrl;
}
