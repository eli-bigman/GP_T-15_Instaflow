package com.insightflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/** Maps the raw JSON product payload returned by the ShopSmart external API. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductCatalogueDto {

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("category")
    private String category;

    @JsonProperty("brand")
    private String brand;

    @JsonProperty("unit_of_measure")
    private String unitOfMeasure;

    /** Defaults to true if absent from the API payload */
    @JsonProperty("is_active")
    private Boolean isActive;
}
