package com.studytimer.service;

import com.studytimer.model.Task;
import com.studytimer.model.User;
import com.studytimer.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final GamificationService gamificationService;

    public List<Task> getAllActiveTasks(User user) {
        return taskRepository.findByUserAndIsCompletedFalseOrderByPriorityAscCreatedAtAsc(user);
    }

    public List<Task> getAllCompletedTasks(User user) {
        return taskRepository.findByUserAndIsCompletedTrueOrderByCompletedAtDesc(user);
    }

    public Task createTask(User user, Task task) {
        task.setUser(user);
        task.setCreatedAt(LocalDateTime.now());
        task.setIsCompleted(false);
        task.setCompletedPomodoros(0);
        return taskRepository.save(task);
    }

    public Task updateTask(User user, Long id, Task taskDetails) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setPriority(taskDetails.getPriority());
        task.setEstimatedPomodoros(taskDetails.getEstimatedPomodoros());

        return taskRepository.save(task);
    }

    @Transactional
    public Task incrementPomodoros(User user, Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        task.setCompletedPomodoros(task.getCompletedPomodoros() + 1);
        return taskRepository.save(task);
    }

    @Transactional
    public Task completeTask(User user, Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        task.setIsCompleted(true);
        task.setCompletedAt(LocalDateTime.now());
        Task savedTask = taskRepository.save(task);

        gamificationService.addTaskCompletionXp(user);

        return savedTask;
    }

    @Transactional
    public Task uncompleteTask(User user, Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        task.setIsCompleted(false);
        task.setCompletedAt(null);
        return taskRepository.save(task);
    }

    public void deleteTask(User user, Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        taskRepository.deleteById(id);
    }

    public Task getTaskById(User user, Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (!task.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        return task;
    }

    public Task saveTask(Task task) {
        return taskRepository.save(task);
    }
}
