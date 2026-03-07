package com.insightflow.controller;

import com.insightflow.dto.*;
import org.springframework.web.bind.annotation.*;
import com.insightflow.model.User;
import com.insightflow.service.PipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;

@RestController @RequestMapping("/api/pipelines") @RequiredArgsConstructor
public class PipelineController {
    private final PipelineService pipelineService;

    @GetMapping
    public ResponseEntity<List<PipelineResponse>> getAll() {
        return ResponseEntity.ok(pipelineService.getAll());
    }

    @PostMapping
    public ResponseEntity<PipelineResponse> create(
            @Valid @RequestBody PipelineRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pipelineService.create(request, user));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<PipelineResponse> run(@PathVariable Long id) {
        return ResponseEntity.ok(pipelineService.runPipeline(id));
    }

    // TODO: Add GET /{id}/reports endpoint
    // TODO: Add scheduled pipeline endpoint
}
