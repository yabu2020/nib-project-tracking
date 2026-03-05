package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String taskName;
    
    @Column(length = 1000)
    private String description;
 private Integer completionPercentage = 0; 
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "tasks", "milestones", "attachments", "initiatedBy", "manager"})
    private Project project;
    
  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "assignedTasks", "initiatedProjects", "managedProjects"})
    private User assignedTo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "assignedTasks", "initiatedProjects", "managedProjects"})
    private User assignedBy;
    
    private LocalDate dueDate;
    private LocalDate completedDate;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;
    
    private Integer priority = 1; 
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    
    @JsonProperty("projectId")
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }
    
    @JsonProperty("assignedToId")
    public Long getAssignedToId() {
        return assignedTo != null ? assignedTo.getId() : null;
    }
    
    @JsonProperty("assignedById")
    public Long getAssignedById() {
        return assignedBy != null ? assignedBy.getId() : null;
    }
    
   
    
    @JsonProperty("projectName")
    public String getProjectNameForJson() {
        return project != null ? project.getProjectName() : null;
    }
    
    @JsonProperty("assignedToName")
    public String getAssignedToNameForJson() {
        return assignedTo != null ? assignedTo.getFullName() : null;
    }
    
    @JsonProperty("project")
    public Map<String, Object> getProjectForJson() {
        if (project == null) return null;
        Map<String, Object> projectMap = new HashMap<>();
        projectMap.put("id", project.getId());
        projectMap.put("projectName", project.getProjectName());
        projectMap.put("status", project.getStatus());
        projectMap.put("ragStatus", project.getRagStatus());
        return projectMap;
    }
    @JsonProperty("assignedTo")
    public Map<String, Object> getAssignedToForJson() {
        if (assignedTo == null) return null;
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", assignedTo.getId());
        userMap.put("fullName", assignedTo.getFullName());
        userMap.put("username", assignedTo.getUsername());
        userMap.put("role", assignedTo.getRole());
        return userMap;
    }
    
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  
    private List<Attachment> attachments = new ArrayList<>();
  
    @JsonProperty("attachmentCount")
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TaskStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        BLOCKED,
        CANCELLED
    }
}