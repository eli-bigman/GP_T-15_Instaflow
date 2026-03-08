package com.insightflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_catalogue")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductCatalogue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku", nullable = false, unique = true, length = 20)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
