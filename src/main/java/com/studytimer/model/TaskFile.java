package com.studytimer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String fileData;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String textContent;

    @ManyToOne
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
