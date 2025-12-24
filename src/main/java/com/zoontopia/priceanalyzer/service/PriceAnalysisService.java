package com.zoontopia.priceanalyzer.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class
PriceAnalysisService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public PriceAnalysisService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public AnalysisResult analyze(String productName, BigDecimal userPrice) {
        // 1. Find similar products
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(productName)
                        .topK(10)
                        .build()
        );

        // 2. Prepare Context for LLM
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        // 3. Construct Prompt
        String prompt = String.format("""
                당신은 가격 전략 전문가입니다.
                답변은 반드시 한국어로 응답해주세요.
                보고서 형식으로 응답해주세요.
                
                사용자는 "%s" 이라는 제품을 %f 원에 판매하고자 합니다.
                
                다음은 현재 시장에 있는 유사 제품 목록입니다 (맥락):
                %s
                
                사용자의 가격을 시장과 비교하여 분석해 주십시오.
                각 이커머스에서 팔고있는 제품 가격을 노출하세요.
                맥락에 있는 유사 제품들의 평균 가격을 계산하십시오.
                사용자의 가격이 경쟁력이 있는지 (너무 높음, 너무 낮음, 또는 적정함) 판단하십시오.
                간략한 추천 사항을 제공하십시오.
                """, productName, userPrice, context);

        System.out.println(prompt);

        // 4. Call LLM
        String aiResponse = chatClient.prompt().user(prompt).call().content();

        return new AnalysisResult(aiResponse, similarDocuments);
    }

    public record AnalysisResult(String analysis, List<Document> similarProducts) {}
}
