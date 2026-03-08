package com.insightflow.repository;

import com.insightflow.model.ProductCatalogue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCatalogueRepository extends JpaRepository<ProductCatalogue, Long> {

    boolean existsBySku(String sku);

    Optional<ProductCatalogue> findBySku(String sku);
}
