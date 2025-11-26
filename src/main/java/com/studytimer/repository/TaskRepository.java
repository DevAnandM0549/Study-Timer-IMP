package com.studytimer.repository;

import com.studytimer.model.Task;
import com.studytimer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserAndIsCompletedFalseOrderByPriorityAscCreatedAtAsc(User user);

    List<Task> findByUserAndIsCompletedTrueOrderByCompletedAtDesc(User user);

    Long countByUserAndIsCompletedTrue(User user);
}
