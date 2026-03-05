package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "apis")
@Data
@NoArgsConstructor
public class Api {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String apiName;
    
    @Column(nullable = false)
    private String apiVersion;
    
    @Column(length = 2000)
    private String description;
    
    @Column(length = 500)
    private String endpoint;
    
    @Column(length = 500)
    private String documentationUrl;
    
    @Enumerated(EnumType.STRING)
    private ApiLifecycleStage lifecycleStage = ApiLifecycleStage.DOCUMENTATION;
    
    @Enumerated(EnumType.STRING)
    private ApiStatus status = ApiStatus.ACTIVE;
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToOne
    @JoinColumn(name = "owner_id")
    @JsonIgnore
    private User owner;
    
    private LocalDate developmentStartDate;
    private LocalDate testingStartDate;
    private LocalDate productionDate;
    
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
    
   
    public enum ApiLifecycleStage {
        DOCUMENTATION,   
        DEVELOPMENT,      
        TESTING,         
        SANDBOXING,       
        UAT,              
        PRODUCTION        
    }
   
    public enum ApiStatus {
        ACTIVE,
        DEPRECATED,
        RETIRED,
        DRAFT
    }
}