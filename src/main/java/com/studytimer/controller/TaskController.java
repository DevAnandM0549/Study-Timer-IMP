package com.studytimer.controller;

import com.studytimer.model.Task;
import com.studytimer.model.TaskFile;
import com.studytimer.model.User;
import com.studytimer.repository.TaskFileRepository;
import com.studytimer.repository.UserRepository;
import com.studytimer.service.TaskService;
import com.studytimer.controller.CreateTaskRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskFileRepository taskFileRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User getAuthenticatedUser(Authentication authentication, HttpServletRequest request) {
        if (authentication != null) {
            return userRepository.findByUsername(authentication.getName()).orElse(null);
        }

        // Manual Basic Auth parsing
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                String[] values = credentials.split(":", 2);
                String username = values[0];
                String rawPassword = values.length > 1 ? values[1] : "";
                User user = userRepository.findByUsername(username).orElse(null);
                if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
                    return user;
                }
            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    // Keep old method for backward compatibility with createTask
    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null) return null;
        return userRepository.findByUsername(authentication.getName())
                .orElse(null);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Task>> getActiveTasks(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(taskService.getAllActiveTasks(user));
    }

    @GetMapping("/completed")
    public ResponseEntity<List<Task>> getCompletedTasks(Authentication authentication, HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(taskService.getAllCompletedTasks(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(taskService.getTaskById(user, id));
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestBody CreateTaskRequest createRequest,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication);

        // If Spring Security didn't populate Authentication (common if httpBasic not enabled),
        // try to authenticate manually from the Authorization header so the API works with the frontend's Basic header.
        if (user == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Basic ")) {
                try {
                    String base64Credentials = authHeader.substring("Basic ".length());
                    String credentials = new String(java.util.Base64.getDecoder().decode(base64Credentials));
                    String[] values = credentials.split(":", 2);
                    String username = values[0];
                    String rawPassword = values.length > 1 ? values[1] : "";
                    user = userRepository.findByUsername(username).orElse(null);
                    if (user != null && passwordEncoder.matches(rawPassword, user.getPassword())) {
                        // authenticated
                    } else {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                    }
                } catch (Exception ex) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        System.out.println("CreateTask called by user=" + (user != null ? user.getUsername() : "<anon>") + " payload=" + createRequest);

        // Build Task from DTO, converting minutes -> pomodoros
        Task task = new Task();
        task.setTitle(createRequest.getTitle());
        task.setDescription(createRequest.getDescription());
        task.setPriority(createRequest.getPriority() != null ? createRequest.getPriority() : Task.Priority.MEDIUM);
        int minutes = (createRequest.getEstimatedMinutes() != null) ? createRequest.getEstimatedMinutes() : 25;
        int estimatedPomodoros = Math.max(1, (int) Math.ceil(minutes / 25.0));
        task.setEstimatedPomodoros(estimatedPomodoros);
        task.setEstimatedMinutes(minutes);

        Task created = taskService.createTask(user, task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<Task>> getTasksForUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        // Return tasks for the authenticated user. Ignore the path id and avoid 403
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        System.out.println("getTasksForUser called. pathId=" + id + " authenticatedId=" + user.getId());

        List<Task> active = taskService.getAllActiveTasks(user);
        List<Task> completed = taskService.getAllCompletedTasks(user);
        List<Task> all = new ArrayList<>();
        all.addAll(active);
        all.addAll(completed);

        return ResponseEntity.ok(all);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @RequestBody Task task,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Task updated = taskService.updateTask(user, id, task);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/increment-pomodoros")
    public ResponseEntity<Task> incrementPomodoros(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Task updated = taskService.incrementPomodoros(user, id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<Task> completeTask(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Task completed = taskService.completeTask(user, id);
        return ResponseEntity.ok(completed);
    }

    @PutMapping("/{id}/uncomplete")
    public ResponseEntity<Task> uncompleteTask(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Task uncompleted = taskService.uncompleteTask(user, id);
        return ResponseEntity.ok(uncompleted);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        taskService.deleteTask(user, id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FILE ENDPOINTS ====================

    @GetMapping("/{taskId}/files")
    public ResponseEntity<List<Map<String, Object>>> getTaskFiles(
            @PathVariable Long taskId,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Verify task belongs to user
        Task task = taskService.getTaskById(user, taskId);
        if (task == null) return ResponseEntity.notFound().build();

        List<TaskFile> files = taskFileRepository.findByTaskId(taskId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskFile file : files) {
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", file.getId());
            fileMap.put("name", file.getFileName());
            fileMap.put("type", file.getFileType());
            fileMap.put("data", file.getFileData());
            fileMap.put("textContent", file.getTextContent());
            result.add(fileMap);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{taskId}/files")
    public ResponseEntity<Map<String, Object>> uploadTaskFile(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> fileData,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Verify task belongs to user
        Task task = taskService.getTaskById(user, taskId);
        if (task == null) return ResponseEntity.notFound().build();

        TaskFile taskFile = new TaskFile();
        taskFile.setTask(task);
        taskFile.setFileName(fileData.get("name"));
        taskFile.setFileType(fileData.get("type"));
        taskFile.setFileData(fileData.get("data"));
        taskFile.setTextContent(fileData.get("textContent"));

        TaskFile saved = taskFileRepository.save(taskFile);

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("name", saved.getFileName());
        result.put("type", saved.getFileType());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{taskId}/files/{fileId}")
    @Transactional
    public ResponseEntity<Void> deleteTaskFile(
            @PathVariable Long taskId,
            @PathVariable Long fileId,
            Authentication authentication,
            HttpServletRequest request) {
        User user = getAuthenticatedUser(authentication, request);
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Verify task belongs to user
        Task task = taskService.getTaskById(user, taskId);
        if (task == null) return ResponseEntity.notFound().build();

        taskFileRepository.deleteById(fileId);
        return ResponseEntity.noContent().build();
    }
}
