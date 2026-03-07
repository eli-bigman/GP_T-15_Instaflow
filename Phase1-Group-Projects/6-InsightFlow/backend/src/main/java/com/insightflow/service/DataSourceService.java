package com.insightflow.service;

import com.insightflow.dto.*;
import com.insightflow.exception.AppException;
import com.insightflow.model.*;
import com.insightflow.model.enums.DataSourceType;
import com.insightflow.repository.DataSourceRepository;
import com.insightflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service @RequiredArgsConstructor
public class DataSourceService {
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

    private DataSourceResponse toResponse(DataSource ds) {
        return DataSourceResponse.builder()
                .id(ds.getId()).name(ds.getName()).description(ds.getDescription())
                .type(ds.getType().name()).connectionUrl(ds.getConnectionUrl())
                .createdByName(ds.getCreatedBy() != null ? ds.getCreatedBy().getName() : null)
                .createdAt(ds.getCreatedAt()).build();
    }
}
