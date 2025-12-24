package com.zoontopia.priceanalyzer.dto;

import java.util.List;

public record AnalyzeResponse(
    String analysis, 
    String chartImageBase64, 
    List<PriceComparison> comparisonData
) {
    public record PriceComparison(String name, Double price) {}
}