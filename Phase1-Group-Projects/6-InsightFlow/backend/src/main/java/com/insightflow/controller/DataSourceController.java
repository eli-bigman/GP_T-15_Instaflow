package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.DataSourceRequest;
import com.insightflow.dto.DataSourceResponse;
import com.insightflow.model.UserPrincipal;
import com.insightflow.service.DataSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/datasources") @RequiredArgsConstructor
public class DataSourceController {
    private final DataSourceService dataSourceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DataSourceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(dataSourceService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DataSourceResponse>> create(
            @Valid @RequestBody DataSourceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Data source created", dataSourceService.create(request, principal)));
    }
}
