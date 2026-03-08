package com.insightflow.service;

import lombok.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class ValidationService {
    
    private static final List<String> VALID_PAYMENT_METHODS = List.of("CASH", "CREDIT_CARD", "DEBIT_CARD", "MOBILE_PAYMENT");

    public List<ValidationResult> validateRow(Map<String, String> row, List<String> requiredFields) {
        List<ValidationResult> errors = new ArrayList<>();
        
        // 1. Check for required fields
        for (String field : requiredFields) {
            String value = row.get(field);
            if (value == null || value.isBlank()) {
                errors.add(new ValidationResult(field + " is required"));
            }
        }

        // 2. Validate SKU (SM-02: 'Missing SKU on row 14')
        String sku = row.get("sku");
        if (sku != null && sku.isBlank()) {
            errors.add(new ValidationResult("Missing SKU"));
        }

        // 3. Validate Payment Method (SM-02: 'Invalid payment method on row 22')
        String paymentMethod = row.get("payment_method");
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            if (!VALID_PAYMENT_METHODS.contains(paymentMethod.toUpperCase())) {
                errors.add(new ValidationResult("Invalid payment method: " + paymentMethod));
            }
        }

        // 4. Validate Quantity (Must be a positive integer)
        String quantityStr = row.get("quantity");
        if (quantityStr != null && !quantityStr.isBlank()) {
            try {
                int quantity = Integer.parseInt(quantityStr);
                if (quantity <= 0) {
                    errors.add(new ValidationResult("Quantity must be greater than zero"));
                }
            } catch (NumberFormatException e) {
                errors.add(new ValidationResult("Quantity must be a valid number"));
            }
        }

        return errors;
    }
    
    @Getter @AllArgsConstructor
    public static class ValidationResult {
        private String errorMessage;
    }
}
