package com.insightflow.repository;

import com.insightflow.model.DataSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DataSourceRepository extends JpaRepository<DataSource, Long> {
    List<DataSource> findAllByOrderByCreatedAtDesc();
}
