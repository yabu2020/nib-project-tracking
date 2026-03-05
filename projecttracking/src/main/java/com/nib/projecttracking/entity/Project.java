package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;  
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;  
import java.util.List;  

@Entity
@Table(name = "projects")
@Data
@NoArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String projectName;
    
    private String projectType;
    
    private String description;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private LocalDate actualEndDate;
    
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;
    
    @Enumerated(EnumType.STRING)
    private RagStatus ragStatus;
    
    private Integer completionPercentage;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by")
    @JsonBackReference("project-initiatedBy") 
    private User initiatedBy;
    
    @ManyToOne
    @JoinColumn(name = "manager_id")
    @JsonManagedReference("project-manager") 
    private User manager;
    
    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  
    private java.util.List<Task> tasks;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private java.util.List<Milestone> milestones;
    
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore  
    private List<Attachment> attachments = new ArrayList<>();
    
    @JsonProperty("attachmentCount")
    public int getAttachmentCount() {
        return attachments != null ? attachments.size() : 0;
    }
    
    public enum ProjectStatus {
        PLANNED, IN_PROGRESS, ON_HOLD, COMPLETED, CANCELLED
    }
    
    public enum RagStatus {
        GREEN, AMBER, RED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ProjectStatus.PLANNED;
        if (ragStatus == null) ragStatus = RagStatus.GREEN;
        if (completionPercentage == null) completionPercentage = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}