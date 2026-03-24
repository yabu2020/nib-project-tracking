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
@Table(name = "projects", uniqueConstraints = {
    @UniqueConstraint(columnNames = "project_name", name = "uk_project_name")
})
@Data
@NoArgsConstructor
public class Project {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "project_name", nullable = false, length = 255)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = true) 
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonBackReference("project-approvedBy")
    private User approvedBy;

    private LocalDateTime approvedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "vpn_status", nullable = true)  
    private VpnStatus vpnStatus;
    
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
    
    public enum VpnStatus {
        NONE,           
        REQUESTED,     
        CONFIGURED      
    }
    
    public enum ApprovalStatus {
        PENDING,     
        APPROVED,     
        REJECTED      
    }

    public ApprovalStatus getApprovalStatus() {
        return approvalStatus != null ? approvalStatus : ApprovalStatus.PENDING;
    }

    public void setApprovalStatus(ApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }
    
    public VpnStatus getVpnStatus() {
        return vpnStatus != null ? vpnStatus : VpnStatus.NONE;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ProjectStatus.PLANNED;
        if (ragStatus == null) ragStatus = RagStatus.GREEN;
        if (completionPercentage == null) completionPercentage = 0;
        if (vpnStatus == null) vpnStatus = VpnStatus.NONE;
        if (approvalStatus == null) approvalStatus = ApprovalStatus.PENDING;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}