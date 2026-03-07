package com.insightflow.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {
    private final String status;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder().status("success").message(message).data(data).build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status("success").message("OK").data(data).build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().status("error").message(message).data(null).build();
    }
}
