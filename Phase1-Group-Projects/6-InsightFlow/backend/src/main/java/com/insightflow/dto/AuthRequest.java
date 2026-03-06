package com.insightflow.dto;

import lombok.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuthRequest {
    @Email @NotBlank private String email;
    @NotBlank private String password;
}
