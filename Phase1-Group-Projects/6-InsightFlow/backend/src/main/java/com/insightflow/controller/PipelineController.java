package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.PipelineRequest;
import com.insightflow.dto.PipelineResponse;
import com.insightflow.model.UserPrincipal;
import com.insightflow.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/pipelines") @RequiredArgsConstructor
public class PipelineController {
    private final PipelineService pipelineService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PipelineResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(pipelineService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PipelineResponse>> create(
            @Valid @RequestBody PipelineRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Pipeline created", pipelineService.create(request, principal)));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<ApiResponse<PipelineResponse>> run(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Pipeline started", pipelineService.runPipeline(id)));
    }

    // TODO: Add GET /{id}/reports endpoint
    // TODO: Add scheduled pipeline endpoint
}
