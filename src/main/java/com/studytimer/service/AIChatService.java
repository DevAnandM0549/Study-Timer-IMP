package com.studytimer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIChatService {

    @Value("${perplexity.api.key}")
    private String apiKey;

    @Value("${perplexity.api.url}")
    private String apiUrl;
    
    @Value("${openrouter.api.url:https://openrouter.ai/api/v1/chat/completions}")
    private String openRouterUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String userMessage) {
        try {
            log.info("=== Perplexity API Call ===");
            log.info("Message: {}", userMessage);
            log.info("API Key (first 15 chars): {}...", apiKey.substring(0, Math.min(15, apiKey.length())));
            log.info("API URL: {}", apiUrl);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey.trim());

            // Minimal Perplexity API request
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-sonar-small-128k-online");
            requestBody.put("messages", List.of(
                Map.of(
                    "role", "user",
                    "content", userMessage
                )
            ));

            log.info("Request: {}", requestBody);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                Map.class
            );

            log.info("Response Status: {}", response.getStatusCode());
            log.info("Response Body: {}", response.getBody());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    log.info("✅ AI Response received successfully");
                    return content;
                }
            }

            log.warn("⚠️ Unexpected response format");
            return "I received a response but couldn't parse it. Please try again.";

        } catch (HttpClientErrorException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("❌ HTTP Error: Status={}, Body={}", e.getStatusCode(), errorBody);
            
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return "🔑 Authentication Error\n\nThe API key appears to be invalid for this endpoint. Please verify:\n1. The key is correct\n2. The key has proper permissions\n3. You're using the right Perplexity API endpoint\n\nError: " + errorBody;
            }
            
            return "API Error (" + e.getStatusCode() + "): " + (errorBody.isEmpty() ? e.getMessage() : errorBody);
            
        } catch (Exception e) {
            log.error("❌ Unexpected error: ", e);
            return "Unexpected error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }
}
