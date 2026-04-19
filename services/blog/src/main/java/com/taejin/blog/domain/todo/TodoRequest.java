package com.taejin.blog.domain.todo;

import jakarta.validation.constraints.NotBlank;

public class TodoRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String description;

    private TodoStatus status;

    private String phase;

    public TodoRequest() {}

    public TodoRequest(String title, String description, TodoStatus status, String phase) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.phase = phase;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TodoStatus getStatus() { return status; }
    public void setStatus(TodoStatus status) { this.status = status; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
}
