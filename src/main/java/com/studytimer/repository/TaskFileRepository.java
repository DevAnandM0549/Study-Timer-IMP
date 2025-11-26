package com.studytimer.repository;

import com.studytimer.model.TaskFile;
import com.studytimer.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {
    List<TaskFile> findByTask(Task task);
    List<TaskFile> findByTaskId(Long taskId);
    void deleteByTaskId(Long taskId);
}
