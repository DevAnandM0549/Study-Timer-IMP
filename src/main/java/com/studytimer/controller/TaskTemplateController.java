package com.studytimer.controller;

import com.studytimer.model.Task;
import com.studytimer.model.TaskTemplate;
import com.studytimer.model.User;
import com.studytimer.service.TaskTemplateService;
import com.studytimer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/templates")
@CrossOrigin(origins = "http://localhost:3000")
public class TaskTemplateController {
    
    @Autowired
    private TaskTemplateService templateService;
    
    @Autowired
    private UserService userService;
    
    // Get all active templates
    @GetMapping
    public ResponseEntity<List<TaskTemplate>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }
    
    // Get templates by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<TaskTemplate>> getTemplatesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(templateService.getTemplatesByCategory(category));
    }
    
    // Get single template
    @GetMapping("/{id}")
    public ResponseEntity<TaskTemplate> getTemplate(@PathVariable Long id) {
        return templateService.getTemplate(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // Create task from template (NO ADDITIONAL AUTH NEEDED - already authenticated)
    @PostMapping("/{templateId}/create-task")
    public ResponseEntity<Task> createTaskFromTemplate(
            @PathVariable Long templateId,
            Authentication authentication) {
        
        // Get current user from authentication
        User user = userService.getUserByUsername(authentication.getName());
        
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Task newTask = templateService.createTaskFromTemplate(templateId, user);
        return ResponseEntity.ok(newTask);
    }
    
    // Create new template (Admin only - optional)
    @PostMapping
    public ResponseEntity<TaskTemplate> createTemplate(@RequestBody TaskTemplate template) {
        TaskTemplate createdTemplate = templateService.createTemplate(template);
        return ResponseEntity.ok(createdTemplate);
    }
    
    // Update template (Admin only - optional)
    @PutMapping("/{id}")
    public ResponseEntity<TaskTemplate> updateTemplate(
            @PathVariable Long id,
            @RequestBody TaskTemplate templateDetails) {
        TaskTemplate updatedTemplate = templateService.updateTemplate(id, templateDetails);
        return ResponseEntity.ok(updatedTemplate);
    }
}
