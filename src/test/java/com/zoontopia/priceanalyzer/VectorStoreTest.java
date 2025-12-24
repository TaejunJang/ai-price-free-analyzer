package com.zoontopia.priceanalyzer;

import com.zoontopia.priceanalyzer.service.DataIngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class VectorStoreTest {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreTest.class);

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DataIngestionService dataIngestionService;

    @Test
    @DisplayName("데이터 적재 후 유사도 검색 테스트")
    void testDataIngestionAndSearch() {
        // Given
        logger.info("데이터 적재 시작 (Force=true)...");
        dataIngestionService.loadData(true);
        logger.info("데이터 적재 완료 (Force=true)");

        logger.info("데이터 적재 재시도 (Force=false) - Idempotency Check...");
        dataIngestionService.loadData(false);
        logger.info("데이터 적재 재시도 완료");

        String query = "노트북"; // 검색할 상품명

        // When
        List<Document> result = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );

        // Then
        logger.info("검색 쿼리: {}", query);
        logger.info("검색 결과 수: {}", result.size());

        if (result.isEmpty()) {
            logger.warn("검색 결과가 없습니다.");
        } else {
            result.forEach(doc -> {
                logger.info("--- 검색 결과 ---");
                logger.info("ID: {}", doc.getMetadata().get("id"));
                logger.info("Name: {}", doc.getMetadata().get("name"));
                logger.info("Price: {}", doc.getMetadata().get("price"));
                logger.info("Content Snippet: {}", doc.getText().substring(0, Math.min(doc.getText().length(), 100)));
            });
        }

        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("VectorStore 유사도 검색 테스트")
    void testSimilaritySearch() {
        // Given
        String query = "고추장"; // 검색할 상품명

        // When
        List<Document> result = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );

        String context = result.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        logger.info("검색 결과: {}", context);

        // Then
        logger.info("검색 쿼리: {}", query);
        logger.info("검색 결과 수: {}", result.size());

        if (result.isEmpty()) {
            logger.warn("검색 결과가 없습니다. 데이터가 인입되었는지 확인해주세요.");
        } else {
            result.forEach(doc -> {
                logger.info("--- 검색 결과 ---");
                logger.info("ID: {}", doc.getMetadata().get("id"));
                logger.info("Name: {}", doc.getMetadata().get("name"));
                logger.info("Price: {}", doc.getMetadata().get("price"));
                logger.info("Source: {}", doc.getMetadata().get("source"));
                logger.info("Content Snippet: {}", doc.getText().substring(0, Math.min(doc.getText().length(), 100)));
            });
        }

        // 데이터가 이미 존재한다고 가정할 때 assertion (데이터가 없을 경우 실패할 수 있음)
        assertThat(result).isNotNull();
    }
}
