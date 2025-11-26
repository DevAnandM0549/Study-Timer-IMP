package com.studytimer.config;

import com.studytimer.model.Task;
import com.studytimer.model.TaskTemplate;
import com.studytimer.repository.TaskTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private TaskTemplateRepository templateRepository;
    
    @Override
    public void run(String... args) throws Exception {
        // Check if templates already exist
        if (templateRepository.findByIsActiveTrue().isEmpty()) {
            
            // Study Templates
            createTemplate("Study Math", "Study Math Basics", 
                "Focus on fundamental concepts and problem-solving", 
                Task.Priority.HIGH, 4, "Study");
            
            createTemplate("Study Programming", "Code Review Session", 
                "Review and refactor existing code", 
                Task.Priority.HIGH, 5, "Study");
            
            createTemplate("Study DSA", "Data Structures Practice", 
                "Practice algorithms and data structure problems", 
                Task.Priority.MEDIUM, 3, "Study");
            
            // Exercise Templates
            createTemplate("Workout", "Daily Exercise", 
                "Physical exercise and fitness routine", 
                Task.Priority.MEDIUM, 2, "Exercise");
            
            createTemplate("Yoga", "Yoga & Meditation", 
                "Mindfulness and stretching exercises", 
                Task.Priority.LOW, 2, "Exercise");
            
            // Work Templates
            createTemplate("Project Work", "Work on Current Project", 
                "Continue development on active project", 
                Task.Priority.HIGH, 6, "Work");
            
            createTemplate("Code Review", "Review Pull Requests", 
                "Review and approve team pull requests", 
                Task.Priority.MEDIUM, 2, "Work");
            
            // Reading Templates
            createTemplate("Reading", "Read Technical Article", 
                "Read and understand technical documentation", 
                Task.Priority.MEDIUM, 2, "Reading");
            
            createTemplate("Research", "Research Topic", 
                "Deep dive into a specific technology topic", 
                Task.Priority.MEDIUM, 3, "Reading");
        }
    }
    
    private void createTemplate(String templateName, String title, String description,
                               Task.Priority priority, int pomodoros, String category) {
        TaskTemplate template = new TaskTemplate();
        template.setTemplateName(templateName);
        template.setTitle(title);
        template.setDescription(description);
        template.setPriority(priority);
        template.setEstimatedPomodoros(pomodoros);
        template.setCategory(category);
        template.setIsActive(true);
        
        templateRepository.save(template);
    }
}
