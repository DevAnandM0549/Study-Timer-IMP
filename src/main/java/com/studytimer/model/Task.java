package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Column(nullable = false)
    private Integer estimatedPomodoros;

    // Store the actual minutes entered by user
    private Integer estimatedMinutes;

    @Column(nullable = false)
    private Integer completedPomodoros = 0;

    @Column(nullable = false)
    private Boolean isCompleted = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    // Foreign key to User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum Priority {
        HIGH, MEDIUM, LOW
    }
}
