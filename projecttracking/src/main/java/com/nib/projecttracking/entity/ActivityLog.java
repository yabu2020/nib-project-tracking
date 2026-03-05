package com.nib.projecttracking.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "assignedTasks", "initiatedProjects", "managedProjects"})
    private User user;
    
    @Column(nullable = false)
    private String action;  
    
    @Column
    private String entityType; 
    
    @Column
    private Long entityId;  
    
    @Column(columnDefinition = "TEXT")
    private String details;  
    
    @Column
    private String ipAddress;
    
    @Column(updatable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
    
   
    @JsonProperty("username")
    public String getUsername() {
        return user != null ? user.getUsername() : null;
    }
    
    
    @JsonProperty("userFullName")
    public String getUserFullName() {
        return user != null ? user.getFullName() : null;
    }
    
    @JsonProperty("userInfo")
    
   
    public static ActivityLog create(User user, String action, String entityType, 
                                     Long entityId, String details, String ipAddress) {
        ActivityLog log = new ActivityLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setIpAddress(ipAddress);
        return log;
    }
}