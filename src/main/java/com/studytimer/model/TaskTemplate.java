package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String templateName;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Task.Priority priority;
    
    @Column(nullable = false)
    private Integer estimatedPomodoros;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    private String category; // e.g., "Study", "Exercise", "Work", "Reading"
}