package com.studytimer.repository;

import com.studytimer.model.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, Long> {
    List<TaskTemplate> findByIsActiveTrue();
    List<TaskTemplate> findByCategoryAndIsActiveTrue(String category);
}