package com.zoontopia.priceanalyzer.dto;

import java.math.BigDecimal;

public record AnalyzeRequest(String productName, BigDecimal price) {}
