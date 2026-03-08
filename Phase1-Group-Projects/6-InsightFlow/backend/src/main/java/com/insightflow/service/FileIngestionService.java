package com.insightflow.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.insightflow.dto.FileUploadResponse;
import com.insightflow.model.DataSource;
import com.insightflow.model.IngestionJob;
import com.insightflow.model.User;
import com.insightflow.model.enums.JobStatus;
import com.insightflow.model.enums.SourceType;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.repository.DataSourceRepository;
import com.insightflow.repository.IngestionJobRepository;
import com.insightflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private final UserRepository userRepository;
    private final ValidationService validationService;

    private static final List<String> REQUIRED_HEADERS = List.of("transaction_id", "product_id", "quantity", "sku", "payment_method");

    @Value("${app.upload.directory:./uploads}")
    private String uploadDirectory;

    public FileUploadResponse uploadAndParseCsv(MultipartFile file, LocalDate uploadDate, Long userId) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Processing CSV upload: {} for date: {} by user: {}", fileName, uploadDate, userId);

        // SM-01: Cannot upload a file for a future date
        if (uploadDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot upload a file for a future date");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Path uploadPath = Paths.get(uploadDirectory);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(UUID.randomUUID() + "_" + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        List<String> headers;
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

            // SM-18: Strict header validation
            for (String required : REQUIRED_HEADERS) {
                if (!headers.contains(required)) {
                    throw new IllegalArgumentException("Missing required column: " + required);
                }
            }

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) continue;
                totalRows++;
                
                String[] values = line.split(",", -1);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String value = (i < values.length) ? values[i].trim() : "";
                    row.put(headers.get(i).trim(), value);
                }
                
                // SM-02: Validation logic with row numbers
                int finalRowNum = rowNum;
                List<String> rowErrors = validationService.validateRow(row, REQUIRED_HEADERS)
                        .stream().map(vr -> "Row " + finalRowNum + ": " + vr.getErrorMessage()).toList();
                
                if (!rowErrors.isEmpty()) { 
                    failedRows++; 
                    validationErrors.addAll(rowErrors); 
                }
            }
        }

        // SM-16: Associate with user's unique store (using User ID for isolation)
        String storeName = "Store_" + user.getId() + "_" + user.getName().replace(" ", "_");
        DataSource dataSource = dataSourceRepository.findFirstByName(storeName)
                .orElseGet(() -> {
                    DataSource ds = DataSource.builder()
                            .name(storeName)
                            .sourceType(SourceType.CSV)
                            .type(DataSourceType.CSV)
                            .description("Auto-created for user: " + user.getName())
                            .isActive(true)
                            .recordCount(0)
                            .createdBy(user)
                            .build();
                    return dataSourceRepository.save(ds);
                });

        dataSource.setRecordCount(dataSource.getRecordCount() + (totalRows - failedRows));
        dataSource.setLastIngestion(LocalDateTime.now());
        dataSourceRepository.save(dataSource);

        // Simplified status mapping: Any failure results in FAILED status
        JobStatus status = (failedRows == 0) ? JobStatus.COMPLETED : JobStatus.FAILED;

        IngestionJob job = IngestionJob.builder()
                .dataSource(dataSource)
                .fileName(fileName)
                .status(status)
                .recordsProcessed(totalRows - failedRows)
                .recordsFailed(failedRows)
                .validationErrors(validationErrors.isEmpty() ? null : String.join("; ", validationErrors))
                .salesDate(uploadDate)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();
        job = ingestionJobRepository.save(job);

        return FileUploadResponse.builder()
                .jobId(job.getId())
                .fileName(fileName)
                .recordsProcessed(totalRows - failedRows)
                .recordsFailed(failedRows)
                .status(job.getStatus().name())
                .validationErrors(validationErrors)
                .build();
    }

    public List<IngestionJob> getJobsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        String storeName = "Store_" + user.getId() + "_" + user.getName().replace(" ", "_");
        Optional<DataSource> dataSource = dataSourceRepository.findFirstByName(storeName);

        // SM-03: Return empty list if no history exists yet
        if (dataSource.isEmpty()) {
            return Collections.emptyList();
        }

        // SM-03: History shows at minimum the last 30 days of uploads
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return ingestionJobRepository.findByDataSourceAndStartedAtAfterOrderByStartedAtDesc(dataSource.get(), thirtyDaysAgo);
    }

    public Optional<IngestionJob> getJobById(Long id) { return ingestionJobRepository.findById(id); }
}
