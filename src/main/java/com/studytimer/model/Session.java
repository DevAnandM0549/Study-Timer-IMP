package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private Integer xpEarned;

    private Long taskId;

    @Column(nullable = false)
    private LocalDateTime completedAt = LocalDateTime.now();

    // Foreign key to User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum SessionType {
        WORK, SHORT_BREAK, LONG_BREAK
    }
}
