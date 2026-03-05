package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_notifications")
@Data
@NoArgsConstructor
public class UserNotification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-notifications")
    private User user;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String message;
    
    @Column(nullable = false)
    private String type; 
    
    @Column(nullable = false)
    private String priority;  
    
    @Column(nullable = false)
    private boolean read = false;
    
    @Column(name = "related_entity_type")
    private String relatedEntityType; 
    
    @Column(name = "related_entity_id")
    private Long relatedEntityId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    
    public void markAsRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
    
    
    public String getPriorityColor() {
        return switch (priority) {
            case "URGENT" -> "#dc3545";  
            case "HIGH" -> "#fd7e14";    
            case "MEDIUM" -> "#ffc107";  
            case "LOW" -> "#28a745";     
            default -> "#6c757d";        
        };
    }
    
   
    public String getIcon() {
        return switch (type) {
            case "TASK_OVERDUE" -> "⚠️";
            case "MILESTONE_OVERDUE" -> "🎯";
            case "PROJECT_UPDATE" -> "📁";
            case "COMMENT" -> "💬";
            case "ASSIGNED" -> "👤";
            case "COMPLETED" -> "✅";
            default -> "🔔";
        };
    }
}