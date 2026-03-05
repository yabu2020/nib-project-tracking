
package com.nib.projecttracking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    @JsonIgnore 
    private String password;
    
    @Column(nullable = false)
    @JsonProperty("fullName")  
    private String fullName;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty("role")  
    private Role role;
    
    private String department;
    
    @Column(nullable = false)
    @JsonProperty("active")  
    private boolean active = true;
    
    @Column(name = "must_reset_password")
    @JsonProperty("mustResetPassword")  
    private Boolean mustResetPassword = true;
    
    @Column(updatable = false)
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    
    @JsonProperty("updatedAt")
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
    
    
    @JsonProperty("mustResetPassword")
    public Boolean getMustResetPassword() {
        return mustResetPassword;
    }
    
    public void setMustResetPassword(Boolean mustResetPassword) {
        this.mustResetPassword = mustResetPassword;
    }
    
   
    @JsonProperty("active")
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public enum Role {
        CEO,
        DEPUTY_CHIEF,
        DIRECTOR,
        BUSINESS,
        QUALITY_ASSURANCE,
        SECURITY,
        DIGITAL_BANKING_MANAGER,
        SENIOR_IT_OFFICER,
        JUNIOR_IT_OFFICER,
        IT_GRADUATE_TRAINEE,
        PROJECT_MANAGER,
        DEVELOPER
    }
}