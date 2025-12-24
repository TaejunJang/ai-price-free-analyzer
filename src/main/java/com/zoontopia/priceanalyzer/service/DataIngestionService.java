package com.zoontopia.priceanalyzer.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zoontopia.priceanalyzer.model.Product;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DataIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;

    @Value("classpath:products.json")
    private Resource productsResource;

    public DataIngestionService(VectorStore vectorStore, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadData(false);
    }

    public void loadData(boolean force) {
        try {
            logger.info("Loading products from JSON...");
            List<Product> products = objectMapper.readValue(productsResource.getInputStream(), new TypeReference<>() {});
            
            if (products.isEmpty()) {
                return;
            }

            if (!force) {
                // Check if data already exists to avoid re-embedding on every restart
                Product firstProduct = products.get(0);
                try {
                    List<Document> existing = vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(firstProduct.name())
                                    .topK(1)
                                    .build()
                    );
    
                    if (!existing.isEmpty()) {
                        logger.info("Data for '{}' found. Skipping initial data ingestion.", firstProduct.name());
                        return;
                    }
                } catch (Exception e) {
                    // Ignore search errors (e.g., collection doesn't exist yet) and proceed to ingest
                    logger.warn("Could not check existing data, proceeding with ingestion: {}", e.getMessage());
                }
            }

            logger.info("Found {} products. Converting to embeddings...", products.size());

           List<Document> documents = products.stream()
                    .map(product -> {
                        String description = product.description() != null ? product.description() : "";
                        String content = "Name: " + product.name() + "\n" +
                                         "Description: " + description + "\n" +
                                         "Category: " + product.category() + "\n" +
                                         "Price: " + product.price() + "\n" +
                                         "Brand: " + product.brand() + "\n" +
                                         "Source: " + product.source();

                        Map<String, Object> metadata = new java.util.HashMap<>();
                        metadata.put("id", product.id());
                        metadata.put("name", product.name());
                        metadata.put("price", product.price().doubleValue());
                        metadata.put("category", product.category());

                        // Use a deterministic UUID based on Product ID for Document ID
                        String documentId = java.util.UUID.nameUUIDFromBytes(product.id().getBytes()).toString();

                        // Use Product ID as Document ID for idempotency (upsert behavior)
                        return new Document(documentId, content, metadata);
                    })
                    .collect(Collectors.toList());

            vectorStore.add(documents);
            logger.info("Successfully ingested {} products into Qdrant.", documents.size());

        } catch (IOException e) {
            logger.error("Failed to ingest data", e);
        }
    }
}
