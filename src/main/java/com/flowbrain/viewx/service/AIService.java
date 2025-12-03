package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.pojo.entity.Video;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Service Integration
 * Uses OpenAI-compatible API (e.g., Alibaba Bailian / DashScope)
 * Note: This assumes spring-ai-openai is used, but implementing via RestTemplate for portability if dependency is missing.
 */
@Service
public class AIService {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Generate video summary and tags using LLM
     */
    public Result<Map<String, Object>> analyzeVideoContent(String title, String description) {
        if (apiKey == null || apiKey.isEmpty()) {
            return Result.error(500, "AI API Key not configured");
        }

        String prompt = String.format(
            "Analyze the following video content:\nTitle: %s\nDescription: %s\n" +
            "1. Provide a concise summary (max 50 words).\n" +
            "2. Generate 5 relevant tags.\n" +
            "3. Analyze sentiment (score -1.0 to 1.0).\n" +
            "Return JSON format: { \"summary\": \"...\", \"tags\": [\"...\"], \"sentiment\": 0.8 }",
            title, description
        );

        try {
            // Construct OpenAI Chat Completion Request
            Map<String, Object> request = new HashMap<>();
            request.put("model", "qwen-turbo"); // Or any other model
            request.put("messages", List.of(
                Map.of("role", "system", "content", "You are a helpful AI assistant for video content analysis. Always return valid JSON."),
                Map.of("role", "user", "content", prompt)
            ));
            request.put("response_format", Map.of("type", "json_object"));

            // Headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(request, headers);

            // Call API
            Map<String, Object> response = restTemplate.postForObject(baseUrl + "/chat/completions", entity, Map.class);
            
            // Parse Response (Simplified)
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    // In a real app, use ObjectMapper to parse 'content' JSON string to Map
                    // For now returning the raw content string or a simple map
                    Map<String, Object> result = new HashMap<>();
                    result.put("raw_analysis", content);
                    return Result.success(result);
                }
            }
            return Result.error(500, "AI API returned empty response");

        } catch (Exception e) {
            return Result.error(500, "AI Analysis failed: " + e.getMessage());
        }
    }

    /**
     * Generate Embedding for Vector Search
     */
    public List<Double> generateEmbedding(String text) {
        if (apiKey == null || apiKey.isEmpty()) return List.of();

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("model", "text-embedding-v1");
            request.put("input", text);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(request, headers);

            Map<String, Object> response = restTemplate.postForObject(baseUrl + "/embeddings", entity, Map.class);

            if (response != null && response.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (!data.isEmpty()) {
                    return (List<Double>) data.get(0).get("embedding");
                }
            }
        } catch (Exception e) {
            System.err.println("Embedding generation failed: " + e.getMessage());
        }
        return List.of();
    }
}
