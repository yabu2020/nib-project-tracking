package com.nib.projecttracking.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "milestones")
@Data
@NoArgsConstructor
public class Milestone {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String milestoneName;
    
    @Column(length = 1000)
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    
    private LocalDate targetDate;
    private LocalDate actualDate;
    
    @Enumerated(EnumType.STRING)
    private MilestoneStatus status = MilestoneStatus.PLANNED;
    
    private Integer completionPercentage = 0;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    
    public enum MilestoneStatus {
        PLANNED,
        IN_PROGRESS,
        COMPLETED,
        DELAYED,
        CANCELLED
    }
}