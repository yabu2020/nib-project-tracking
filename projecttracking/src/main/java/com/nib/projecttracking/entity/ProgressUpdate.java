package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "progress_updates")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({
    "hibernateLazyInitializer",
    "handler"
})
public class ProgressUpdate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore  
    private User user;
    
   
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    private LocalDate updateDate;
    
    @Column(length = 2000)
    private String completedWork;
    
    @Column(length = 2000)
    private String ongoingWork;
    
    @Column(length = 2000)
    private String blockers;
    
    private LocalDate estimatedResolution;
    
    @Column(name = "submitted_to_management")
    private Boolean submittedToManagement = false;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    @Column(name = "blocker_status")
private String blockerStatus = "OPEN";  

@Column(name = "solved_by_id")
private Long solvedById;

@Column(name = "solved_by_role")
private String solvedByRole;

@Column(name = "solved_at")
private LocalDateTime solvedAt;

@Column(name = "acknowledged")
private Boolean acknowledged = false;

@Column(name = "acknowledged_by")
private Long acknowledgedBy;

@Column(name = "acknowledged_at")
private LocalDateTime acknowledgedAt;
public Boolean getAcknowledged() { return acknowledged; }
public void setAcknowledged(Boolean acknowledged) { this.acknowledged = acknowledged; }

public Long getAcknowledgedBy() { return acknowledgedBy; }
public void setAcknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; }

public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
public void setAcknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }

public String getBlockerStatus() { return blockerStatus; }
public void setBlockerStatus(String blockerStatus) { this.blockerStatus = blockerStatus; }

public Long getSolvedById() { return solvedById; }
public void setSolvedById(Long solvedById) { this.solvedById = solvedById; }

public String getSolvedByRole() { return solvedByRole; }
public void setSolvedByRole(String solvedByRole) { this.solvedByRole = solvedByRole; }

public LocalDateTime getSolvedAt() { return solvedAt; }
public void setSolvedAt(LocalDateTime solvedAt) { this.solvedAt = solvedAt; }
    
    @JsonProperty("userName")
    public String getUserName() {
        return user != null ? user.getFullName() : "Unknown User";
    }
    
    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getId() : null;
    }
    
  
    @JsonProperty("submittedByUsername")
    public String getSubmittedByUsername() {
        return submittedBy != null ? submittedBy.getUsername() : null;
    }
    
    @JsonProperty("submittedByFullName")
    public String getSubmittedByFullName() {
        return submittedBy != null ? submittedBy.getFullName() : null;
    }
    
    @JsonProperty("submittedByRole")
    public String getSubmittedByRole() {
        return submittedBy != null && submittedBy.getRole() != null 
            ? submittedBy.getRole().name() 
            : null;
    }
    
    @JsonProperty("submittedById")
    public Long getSubmittedById() {
        return submittedBy != null ? submittedBy.getId() : null;
    }
    
  
    @JsonProperty("projectName")
    public String getProjectName() {
        return project != null ? project.getProjectName() : "N/A";
    }
    
    @JsonProperty("projectId")
    public Long getProjectId() {
        return project != null ? project.getId() : null;
    }
    

    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        
        if (submittedBy == null && user != null) {
            submittedBy = user;
        }
        
       
        if (submittedToManagement == null) {
            if (submittedBy != null && submittedBy.getRole() != null) {
                String role = submittedBy.getRole().name();
                submittedToManagement = role.equals("DEVELOPER") || 
                                       role.equals("SENIOR_IT_OFFICER") || 
                                       role.equals("JUNIOR_IT_OFFICER") ||
                                       role.equals("IT_GRADUATE_TRAINEE");
            } else {
                submittedToManagement = false;
            }
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}