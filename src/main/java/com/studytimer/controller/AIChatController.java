package com.studytimer.controller;

import com.studytimer.service.AIChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AIChatController {

    private final AIChatService aiChatService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        
        if (userMessage == null || userMessage.trim().isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Message cannot be empty");
            return ResponseEntity.badRequest().body(error);
        }

        String response = aiChatService.chat(userMessage);
        
        Map<String, String> result = new HashMap<>();
        result.put("response", response);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "AI Chat service is running");
        result.put("message", "Send a POST request to /api/chat with a message to test the AI");
        return ResponseEntity.ok(result);
    }
}
