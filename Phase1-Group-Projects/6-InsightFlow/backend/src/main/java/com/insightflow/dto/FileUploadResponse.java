package com.insightflow.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FileUploadResponse {
    private Long jobId;
    private String fileName;
    private Integer recordsProcessed;
    private Integer recordsFailed;
    private String status;
    private List<String> validationErrors;
}
