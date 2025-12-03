package com.flowbrain.viewx.controller;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.VideoMapper;
import com.flowbrain.viewx.pojo.entity.Video;
import com.flowbrain.viewx.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    @Autowired
    private VideoMapper videoMapper;

    /**
     * AI Content Analysis
     * Triggered manually or after upload
     */
    @PostMapping("/analyze")
    public Result<?> analyzeContent(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String description = payload.get("description");
        return aiService.analyzeVideoContent(title, description);
    }

    /**
     * Semantic Search (RAG)
     * Search videos by natural language query using vector embeddings
     */
    @GetMapping("/search")
    public Result<List<Video>> semanticSearch(@RequestParam String query) {
        // 1. Generate embedding for the user query
        List<Double> embedding = aiService.generateEmbedding(query);
        
        if (embedding == null || embedding.isEmpty()) {
            return Result.error(500, "Failed to generate embedding");
        }

        // 2. Convert List<Double> to pgvector string format "[0.1, 0.2, ...]"
        String vectorStr = "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        // 3. Search in DB
        List<Video> videos = videoMapper.selectByVector(vectorStr, 10);
        
        return Result.success(videos);
    }
}
