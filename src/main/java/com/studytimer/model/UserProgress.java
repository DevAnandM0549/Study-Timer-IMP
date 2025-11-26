package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer totalXp = 0;

    @Column(nullable = false)
    private Integer currentLevel = 1;

    @Column(nullable = false)
    private Integer totalSessions = 0;

    @Column(nullable = false)
    private Integer totalMinutes = 0;

    @Column(nullable = false)
    private Integer completedTasks = 0;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer longestStreak = 0;

    private LocalDateTime lastSessionDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Foreign key to User
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
