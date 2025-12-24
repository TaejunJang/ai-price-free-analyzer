package com.zoontopia.priceanalyzer.controller;

import com.zoontopia.priceanalyzer.dto.AnalyzeRequest;
import com.zoontopia.priceanalyzer.dto.AnalyzeResponse;
import com.zoontopia.priceanalyzer.service.ChartService;
import com.zoontopia.priceanalyzer.service.DataIngestionService;
import com.zoontopia.priceanalyzer.service.PriceAnalysisService;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PriceAnalysisRestController {

    private final PriceAnalysisService priceAnalysisService;
    private final ChartService chartService;
    private final DataIngestionService dataIngestionService;

    public PriceAnalysisRestController(PriceAnalysisService priceAnalysisService, 
                                       ChartService chartService, 
                                       DataIngestionService dataIngestionService) {
        this.priceAnalysisService = priceAnalysisService;
        this.chartService = chartService;
        this.dataIngestionService = dataIngestionService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@RequestBody AnalyzeRequest request) {
        try {
            // 1. Analyze
            PriceAnalysisService.AnalysisResult result = priceAnalysisService.analyze(request.productName(), request.price());

            // 2. Prepare Chart Data for Chart.js
            java.util.List<AnalyzeResponse.PriceComparison> comparisonData = result.similarProducts().stream()
                    .map(doc -> {
                        String name = (String) doc.getMetadata().getOrDefault("name", "Unknown");
                        Double price = (Double) doc.getMetadata().getOrDefault("price", 0.0);
                        return new AnalyzeResponse.PriceComparison(name, price);
                    })
                    .collect(java.util.stream.Collectors.toList());

            // Add user's price as well
            comparisonData.add(0, new AnalyzeResponse.PriceComparison("내 상품 (제안가)", request.price().doubleValue()));

            // 3. Create Chart (Keeping for backward compatibility or removing if not needed - let's keep it for now but we will use the JSON data on UI)
            JFreeChart chart = chartService.createComparisonChart(request.price(), result.similarProducts());
            String base64Image = convertChartToBase64(chart);

            return ResponseEntity.ok(new AnalyzeResponse(result.analysis(), base64Image, comparisonData));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetData() {
        dataIngestionService.loadData(true);
        return ResponseEntity.ok(Map.of("message", "Data reset successfully"));
    }

    private String convertChartToBase64(JFreeChart chart) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtils.writeChartAsPNG(bos, chart, 500, 300);
        byte[] bytes = bos.toByteArray();
        return Base64.getEncoder().encodeToString(bytes);
    }
}
