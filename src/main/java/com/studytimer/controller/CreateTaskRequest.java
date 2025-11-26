package com.studytimer.controller;

import com.studytimer.model.Task;

public class CreateTaskRequest {
    private String title;
    private String description;
    private Integer estimatedMinutes;
    private Task.Priority priority = Task.Priority.MEDIUM;

    public CreateTaskRequest() {}

    public CreateTaskRequest(String title, String description, Integer estimatedMinutes, Task.Priority priority) {
        this.title = title;
        this.description = description;
        this.estimatedMinutes = estimatedMinutes;
        this.priority = priority;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(Integer estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public Task.Priority getPriority() {
        return priority;
    }

    public void setPriority(Task.Priority priority) {
        this.priority = priority;
    }
}
