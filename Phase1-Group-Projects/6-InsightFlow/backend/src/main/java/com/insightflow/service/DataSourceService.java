package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.model.*;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.repository.DataSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceRepository dataSourceRepository;

    public List<DataSourceResponse> getAll() {
        return dataSourceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse).toList();
    }

    public DataSourceResponse create(DataSourceRequest request, User user) {
        DataSource ds = DataSource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(DataSourceType.valueOf(request.getType()))
                .connectionUrl(request.getConnectionUrl())
                .createdBy(user)
                .build();
        return toResponse(dataSourceRepository.save(ds));
    }

    // TODO: Implement update and delete
    // TODO: Implement test connection method

    private DataSourceResponse toResponse(DataSource ds) {
        return DataSourceResponse.builder()
                .id(ds.getId()).name(ds.getName()).description(ds.getDescription())
                .type(ds.getType().name()).connectionUrl(ds.getConnectionUrl())
                .createdByName(ds.getCreatedBy() != null ? ds.getCreatedBy().getName() : null)
                .createdAt(ds.getCreatedAt()).build();
    }
}
