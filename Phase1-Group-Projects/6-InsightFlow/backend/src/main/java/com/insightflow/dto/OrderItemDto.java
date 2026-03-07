package com.insightflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;

/** Maps the nested item objects inside the external API order payload. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderItemDto {

    @JsonProperty("order_item_id")
    private String orderItemId;

    @JsonProperty("sku")
    private String sku;

    @JsonProperty("quantity")
    private Integer quantity;

    @JsonProperty("unit_price_ghs")
    private BigDecimal unitPriceGhs;

    @JsonProperty("line_total_ghs")
    private BigDecimal lineTotalGhs;
}
