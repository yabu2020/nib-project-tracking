package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "project_comments")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})  
public class ProjectComment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnore 
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  
    private User user;
    
    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @JsonIgnore  
    private ProjectComment parentComment;
    
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ProjectComment> replies = new ArrayList<>();  
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @JsonProperty("parentCommentId")
    public Long getParentCommentId() {
        return parentComment != null ? parentComment.getId() : null;
    }
    
    @JsonProperty("userName")
    public String getUserName() {
        return user != null ? user.getFullName() : "Unknown User";
    }
    
    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
    @JsonProperty("userRole")
    public String getUserRole() {
        return user != null && user.getRole() != null ? user.getRole().toString() : null;
    }
    
    @JsonProperty("projectId")
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }
    
    @JsonProperty("projectName")
    public String getProjectName() {
        return project != null ? project.getProjectName() : "N/A";
    }
    
    @JsonProperty("replyCount")
    public int getReplyCount() {
        return replies != null ? replies.size() : 0;
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
}