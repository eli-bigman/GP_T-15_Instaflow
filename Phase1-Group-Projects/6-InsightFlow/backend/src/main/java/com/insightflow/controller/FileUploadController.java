package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.FileUploadResponse;
import com.insightflow.model.IngestionJob;
import com.insightflow.model.UserPrincipal;
import com.insightflow.service.FileIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ingest")
@RequiredArgsConstructor
public class FileUploadController {
    private final FileIngestionService fileIngestionService;

    @PostMapping("/csv")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        
        FileUploadResponse response = fileIngestionService.uploadAndParseCsv(file, date, principal.id());
        return ResponseEntity.ok(ApiResponse.success("File uploaded and processed", response));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<List<IngestionJob>>> getJobs(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(fileIngestionService.getJobsForUser(principal.id())));
    }

    @GetMapping("/jobs/{id}")
    public ResponseEntity<ApiResponse<IngestionJob>> getJobById(@PathVariable Long id) {
        return fileIngestionService.getJobById(id)
                .map(job -> ResponseEntity.ok(ApiResponse.success(job)))
                .orElse(ResponseEntity.notFound().build());
    }
}
