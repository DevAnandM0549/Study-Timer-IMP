package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String achievementKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer xpReward;

    @Column(nullable = false)
    private Boolean isUnlocked = false;

    private LocalDateTime unlockedAt;

    // Foreign key to User
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
