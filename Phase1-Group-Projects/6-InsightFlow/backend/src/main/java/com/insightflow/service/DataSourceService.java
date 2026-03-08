package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.exception.AppException;
import com.insightflow.model.*;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.model.enums.SourceType;
import com.insightflow.repository.DataSourceRepository;
import com.insightflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service @RequiredArgsConstructor
public class DataSourceService {
    private static final Logger log = LoggerFactory.getLogger(DataSourceService.class);

    private final DataSourceRepository dataSourceRepository;
    private final UserRepository userRepository;

    public List<DataSourceResponse> getAll() {
        return dataSourceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    public DataSourceResponse create(DataSourceRequest request, UserPrincipal principal) {
        DataSourceType type;
        try {
            type = DataSourceType.valueOf(request.getType());
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Invalid data source type: " + request.getType());
        }
        DataSource ds = DataSource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(type)
                .connectionUrl(request.getConnectionUrl())
                .createdBy(userRepository.getReferenceById(principal.id()))
                .build();
        return toResponse(dataSourceRepository.save(ds));
    }

    // TODO: Implement update and delete
    // TODO: Implement test connection method

    /**
     * Upserts a data_sources entry for a system-managed ingestion source.
     * Creates the entry on first sync; updates last_ingestion, record_count,
     * and is_active on every subsequent sync.
     *
     * @param name         Unique source name (e.g. "ShopSmart Orders API")
     * @param description  Human-readable description
     * @param connectionUrl Base URL of the external API
     * @param type         DataSourceType (API, JSON, CSV, DATABASE)
     * @param sourceType   SourceType (API, JSON, CSV, DATABASE)
     * @param recordCount  Number of records staged in this sync run
     */
    public void recordIngestion(String name, String description, String connectionUrl,
                                DataSourceType type, SourceType sourceType, int recordCount) {
        try {
            DataSource ds = dataSourceRepository.findFirstByName(name)
                    .orElse(DataSource.builder()
                            .name(name)
                            .description(description)
                            .type(type)
                            .sourceType(sourceType)
                            .connectionUrl(connectionUrl)
                            .build());

            ds.setIsActive(true);
            ds.setRecordCount(recordCount);
            ds.setLastIngestion(LocalDateTime.now(ZoneOffset.UTC));
            dataSourceRepository.save(ds);
            log.info("Recorded ingestion for [{}] — {} records", name, recordCount);
        } catch (Exception e) {
            // Non-critical — log and continue; never fail the ingestion pipeline for an audit write
            log.error("Failed to record ingestion for [{}]: {}", name, e.getMessage());
        }
    }

    private DataSourceResponse toResponse(DataSource ds) {
        return DataSourceResponse.builder()
                .id(ds.getId()).name(ds.getName()).description(ds.getDescription())
                .type(ds.getType().name()).connectionUrl(ds.getConnectionUrl())
                .createdByName(ds.getCreatedBy() != null ? ds.getCreatedBy().getName() : null)
                .createdAt(ds.getCreatedAt()).build();
    }
}
