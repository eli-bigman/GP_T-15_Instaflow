package com.insightflow.service;

import lombok.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    
    public List<ValidationResult> validateRow(Map<String, String> row, List<String> requiredFields) {
        List<ValidationResult> errors = new ArrayList<>();
        for (String field : requiredFields) {
            String value = row.get(field);
            if (value == null || value.isBlank()) {
                errors.add(new ValidationResult(field + " is required"));
            }
        }
        return errors;
    }
    
    @Getter @AllArgsConstructor
    public static class ValidationResult {
        private String errorMessage;
    }
}
