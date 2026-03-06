package com.insightflow.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.insightflow.dto.FileUploadResponse;
import com.insightflow.model.DataSource;
import com.insightflow.model.IngestionJob;
import com.insightflow.model.enums.JobStatus;
import com.insightflow.model.enums.SourceType;
import com.insightflow.repository.DataSourceRepository;
import com.insightflow.repository.IngestionJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Service responsible for ingesting CSV file uploads.
 * Owner: Dev A
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileIngestionService {
    private final DataSourceRepository dataSourceRepository;
    private final IngestionJobRepository ingestionJobRepository;
    private final ValidationService validationService;

    @Value("${app.upload.directory:./uploads}")
    private String uploadDirectory;

    public FileUploadResponse uploadAndParseCsv(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Processing CSV upload: {}", fileName);

        Path uploadPath = Paths.get(uploadDirectory);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        List<String> headers;
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();
        int totalRows = 0, failedRows = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("CSV file is empty or has no header row");
            }
            headers = Arrays.asList(headerLine.split(","));
            log.info("Detected {} columns: {}", headers.size(), headers);

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                totalRows++;
                if (line.isBlank()) continue;
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = (i < values.length) ? values[i].trim() : "";
                    row.put(headers.get(i).trim(), value);
                }
                List<String> requiredFields = List.of("transaction_id", "product_id", "quantity");
                int finalRowNum = rowNum;
                List<String> rowErrors = validationService.validateRow(row, requiredFields)
                        .stream().map(vr -> "Row " + finalRowNum + ": " + vr.getErrorMessage()).toList();
                if (!rowErrors.isEmpty()) { failedRows++; validationErrors.addAll(rowErrors); }
                rows.add(row);
            }
        }

        List<DataSource> csvSources = dataSourceRepository.findBySourceType(SourceType.CSV);
        DataSource dataSource;
        if (csvSources.isEmpty()) {
            dataSource = DataSource.builder().name("CSV Upload - " + fileName).sourceType(SourceType.CSV)
                    .description("Auto-created from file upload: " + fileName).isActive(true)
                    .recordCount(totalRows).lastIngestion(LocalDateTime.now()).build();
            dataSource = dataSourceRepository.save(dataSource);
        } else {
            dataSource = csvSources.get(0);
            dataSource.setRecordCount(dataSource.getRecordCount() + totalRows);
            dataSource.setLastIngestion(LocalDateTime.now());
            dataSourceRepository.save(dataSource);
        }

        IngestionJob job = IngestionJob.builder().dataSource(dataSource).fileName(fileName)
                .status(JobStatus.COMPLETED).recordsProcessed(totalRows - failedRows).recordsFailed(failedRows)
                .validationErrors(validationErrors.isEmpty() ? null : String.join("; ", validationErrors))
                .startedAt(LocalDateTime.now()).completedAt(LocalDateTime.now()).build();
        job = ingestionJobRepository.save(job);

        return FileUploadResponse.builder().jobId(job.getId()).fileName(fileName)
                .recordsProcessed(totalRows - failedRows).recordsFailed(failedRows)
                .status(job.getStatus().name()).validationErrors(validationErrors).build();
    }

    public List<IngestionJob> getJobs() { return ingestionJobRepository.findAll(); }
    public Optional<IngestionJob> getJobById(Long id) { return ingestionJobRepository.findById(id); }
}
