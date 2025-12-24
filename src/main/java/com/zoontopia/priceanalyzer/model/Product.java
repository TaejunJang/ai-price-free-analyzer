package com.zoontopia.priceanalyzer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(
    String id, 
    @JsonProperty("product_name") String name, 
    BigDecimal price, 
    String category, 
    String brand,
    String source,
    String description
) {
}
