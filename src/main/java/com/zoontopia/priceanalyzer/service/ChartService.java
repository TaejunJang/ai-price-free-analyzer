package com.zoontopia.priceanalyzer.service;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ChartService {

    public JFreeChart createComparisonChart(BigDecimal userPrice, List<Document> similarProducts) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Add User Price
        dataset.addValue(userPrice, "Price", "Your Price");

        // Calculate Average Market Price
        double averagePrice = similarProducts.stream()
                .map(doc -> {
                    Object priceObj = doc.getMetadata().get("price");
                    if (priceObj instanceof Number) {
                        return ((Number) priceObj).doubleValue();
                    }
                    return 0.0;
                })
                .filter(p -> p > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        if (averagePrice > 0) {
            dataset.addValue(averagePrice, "Price", "Market Average");
        }
        
        // Optionally add individual similar products if needed, but keeping it simple for now
        // for (Document doc : similarProducts) {
        //    String name = (String) doc.getMetadata().get("name");
        //    Double price = ((Number) doc.getMetadata().get("price")).doubleValue();
        //    dataset.addValue(price, "Price", name);
        // }

        return ChartFactory.createBarChart(
                "Price Comparison",
                "Category",
                "Price ($)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
    }
}
