package com.insightflow.controller;

import com.insightflow.dto.*;
import com.insightflow.model.User;
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
    public ResponseEntity<List<DataSourceResponse>> getAll() {
        return ResponseEntity.ok(dataSourceService.getAll());
    }

    @PostMapping
    public ResponseEntity<DataSourceResponse> create(
            @Valid @RequestBody DataSourceRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(dataSourceService.create(request, user));
    }

    // TODO: Add file upload endpoint for CSV/JSON data sources
    // TODO: Add test connection endpoint
}
