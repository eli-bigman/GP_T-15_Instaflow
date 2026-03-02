package com.insightflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PipelineRequest {
    @NotBlank private String name;
    private String description;
    @NotNull private Long dataSourceId;
}
