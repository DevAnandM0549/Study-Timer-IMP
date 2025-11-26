package com.studytimer.service;

import com.studytimer.model.Task;
import com.studytimer.model.TaskTemplate;
import com.studytimer.model.User;
import com.studytimer.repository.TaskTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TaskTemplateService {
    
    @Autowired
    private TaskTemplateRepository templateRepository;
    
    @Autowired
    private TaskService taskService;
    
    // Get all active templates
    public List<TaskTemplate> getAllTemplates() {
        return templateRepository.findByIsActiveTrue();
    }
    
    // Get templates by category
    public List<TaskTemplate> getTemplatesByCategory(String category) {
        return templateRepository.findByCategoryAndIsActiveTrue(category);
    }
    
    // Get single template
    public Optional<TaskTemplate> getTemplate(Long templateId) {
        return templateRepository.findById(templateId);
    }
    
    // Create task from template
    public Task createTaskFromTemplate(Long templateId, User user) {
        Optional<TaskTemplate> template = templateRepository.findById(templateId);
        
        if (template.isEmpty()) {
            throw new RuntimeException("Template not found");
        }
        
        TaskTemplate tmpl = template.get();
        
        // Create new task from template
        Task newTask = new Task();
        newTask.setUser(user);
        newTask.setTitle(tmpl.getTitle());
        newTask.setDescription(tmpl.getDescription());
        newTask.setPriority(tmpl.getPriority());
        newTask.setEstimatedPomodoros(tmpl.getEstimatedPomodoros());
        newTask.setCreatedAt(LocalDateTime.now());
        newTask.setIsCompleted(false);
        newTask.setCompletedPomodoros(0);
        
        return taskService.saveTask(newTask);
    }
    
    // Create new template
    public TaskTemplate createTemplate(TaskTemplate template) {
        template.setCreatedAt(LocalDateTime.now());
        template.setIsActive(true);
        return templateRepository.save(template);
    }
    
    // Update template
    public TaskTemplate updateTemplate(Long id, TaskTemplate templateDetails) {
        Optional<TaskTemplate> template = templateRepository.findById(id);
        
        if (template.isEmpty()) {
            throw new RuntimeException("Template not found");
        }
        
        TaskTemplate tmpl = template.get();
        tmpl.setTemplateName(templateDetails.getTemplateName());
        tmpl.setTitle(templateDetails.getTitle());
        tmpl.setDescription(templateDetails.getDescription());
        tmpl.setPriority(templateDetails.getPriority());
        tmpl.setEstimatedPomodoros(templateDetails.getEstimatedPomodoros());
        tmpl.setCategory(templateDetails.getCategory());
        
        return templateRepository.save(tmpl);
    }
    
    // Deactivate template
    public void deactivateTemplate(Long id) {
        Optional<TaskTemplate> template = templateRepository.findById(id);
        if (template.isPresent()) {
            template.get().setIsActive(false);
            templateRepository.save(template.get());
        }
    }
}