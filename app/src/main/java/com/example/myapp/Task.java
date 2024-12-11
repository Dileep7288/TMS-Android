package com.example.myapp;

import java.util.Date;

public class Task {
    private int id;
    private String status;
    private String priority;
    private Date dueDate;
    private String title;
    private String description;
    private Date createdAt;
    private String user;

    public Task() {
    }

    public Task(int id, String status, String priority, Date dueDate, String title, String description) {
        this.id = id;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.title = title;
        this.description = description;
    }

    public Task(int id, String title, String description, String status, String priority,
                Date dueDate, Date createdAt, String user) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.user = user;
    }

    public int getId() { return id; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Date getDueDate() { return dueDate; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Date getCreatedAt() { return createdAt; }
    public String getUser() { return user; }


    public void setId(int id) { this.id = id; }
    public void setStatus(String status) { this.status = status; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setUser(String user) { this.user = user; }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", deadline=" + dueDate +
                ", created_at=" + createdAt +
                ", user='" + user + '\'' +
                '}';
    }
}