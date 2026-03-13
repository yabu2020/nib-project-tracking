package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.Attachment;
import com.nib.projecttracking.repository.ProjectRepository;
import com.nib.projecttracking.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AttachmentRepository attachmentRepository; 

    @Autowired
    private ActivityLogService activityLogService;

    private User getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
        } catch (Exception e) {
            System.err.println("Error getting current user from security context: " + e.getMessage());
        }
        return null;
    }

    /**
     * Create a new project - LEGACY METHOD (for backward compatibility)
     */
    public Project createProject(Project project, User initiatedBy, User manager) {
        User currentUser = getCurrentUser();
        return createProjectWithUser(project, initiatedBy, manager, currentUser);
    }

    /**
     * Create a new project WITH EXPLICIT CURRENT USER (for reliable logging)
     */
    public Project createProjectWithUser(Project project, User initiatedBy, User manager, User currentUser) {
    System.out.println("=== PROJECT SERVICE - CREATE ===");
    System.out.println("Project name: " + project.getProjectName());
    System.out.println("Initiated by: " + (initiatedBy != null ? initiatedBy.getUsername() : "null"));
    System.out.println("Manager: " + (manager != null ? manager.getUsername() : "null"));
    System.out.println("Current user (for logging): " + (currentUser != null ? currentUser.getUsername() : "null"));

    project.setInitiatedBy(initiatedBy);
    project.setManager(manager);
    if (currentUser != null) {
        project.setCreatedBy(currentUser);
        System.out.println("✅ Set createdBy to: " + currentUser.getUsername());
    } else {
        System.out.println("⚠️ WARNING: currentUser is null - createdBy will be NULL!");
    }
    if (project.getStatus() == null) {
        project.setStatus(Project.ProjectStatus.PLANNED);
    }
    if (project.getRagStatus() == null) {
        project.setRagStatus(Project.RagStatus.GREEN);
    }
    if (project.getCompletionPercentage() == null) {
        project.setCompletionPercentage(0);
    }
    Project savedProject = projectRepository.save(project);
    System.out.println("Project saved with ID: " + savedProject.getId());
    System.out.println("✅ savedProject.createdBy: " + 
        (savedProject.getCreatedBy() != null ? savedProject.getCreatedBy().getUsername() : "NULL"));
    if (currentUser != null) {
        String details = String.format("Created new project '%s' (type: %s) initiated by %s, managed by %s",
                savedProject.getProjectName(),
                savedProject.getProjectType(),
                initiatedBy != null ? initiatedBy.getFullName() : "System",
                manager != null ? manager.getFullName() : "Unassigned");

        try {
            activityLogService.logAction(
                    currentUser,
                    "PROJECT_CREATED",
                    "Project",
                    savedProject.getId(),
                    details
            );
            System.out.println("📝 Project creation logged by: " + currentUser.getUsername());
        } catch (Exception logError) {
            System.err.println("⚠️ Failed to log project creation: " + logError.getMessage());
        }
    } else {
        System.out.println("⚠️ WARNING: currentUser is null - project logging skipped!");
    }

    return savedProject;
}

    /**
     * Update project details - LEGACY METHOD (for backward compatibility)
     */
    public Project updateProject(Long projectId, Project projectDetails) {
        User currentUser = getCurrentUser();
        return updateProjectWithUser(projectId, projectDetails, currentUser);
    }

    /**
     * Update project details WITH EXPLICIT CURRENT USER (for reliable logging)
     */
    public Project updateProjectWithUser(Long projectId, Project projectDetails, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        StringBuilder changes = new StringBuilder();
          boolean canEditDates = currentUser != null && (
        currentUser.getRole() == User.Role.PROJECT_MANAGER ||
        currentUser.getRole() == User.Role.CEO ||
        currentUser.getRole() == User.Role.DEPUTY_CHIEF ||
        currentUser.getRole() == User.Role.DIRECTOR ||
        currentUser.getRole() == User.Role.SECURITY ||
        currentUser.getRole() == User.Role.DIGITAL_BANKING_MANAGER
    );
        if (projectDetails.getProjectName() != null &&
                !projectDetails.getProjectName().equals(project.getProjectName())) {
            changes.append("Name updated; ");
            project.setProjectName(projectDetails.getProjectName());
        }
        if (projectDetails.getProjectType() != null &&
                !projectDetails.getProjectType().equals(project.getProjectType())) {
            changes.append("Type updated; ");
            project.setProjectType(projectDetails.getProjectType());
        }
        if (projectDetails.getDescription() != null &&
                !projectDetails.getDescription().equals(project.getDescription())) {
            changes.append("Description updated; ");
            project.setDescription(projectDetails.getDescription());
        }
    if (canEditDates) {
        if (projectDetails.getStartDate() != null &&
                !projectDetails.getStartDate().equals(project.getStartDate())) {
            changes.append("Start date updated; ");
            project.setStartDate(projectDetails.getStartDate());
        }
        if (projectDetails.getEndDate() != null &&
                !projectDetails.getEndDate().equals(project.getEndDate())) {
            changes.append("End date updated; ");
            project.setEndDate(projectDetails.getEndDate());
        }
    } else if (projectDetails.getStartDate() != null || projectDetails.getEndDate() != null) {
       
        System.out.println("⚠️ User " + (currentUser != null ? currentUser.getUsername() : "unknown") + 
                          " attempted to update project dates without permission");
    }

        if (projectDetails.getStatus() != null &&
                !projectDetails.getStatus().equals(project.getStatus())) {
            changes.append("Status updated; ");
            project.setStatus(projectDetails.getStatus());
        }

        
        Project.RagStatus newRag = calculateRagStatus(project);
        if (project.getRagStatus() != newRag) {
            changes.append("RAG status updated; ");
            project.setRagStatus(newRag);
        }

        Project saved = projectRepository.save(project);

        if (changes.length() > 0 && currentUser != null) {
            String details = "Project updated: " + changes.toString().trim() + " (" + project.getProjectName() + ")";
            try {
                activityLogService.logAction(
                        currentUser,
                        "PROJECT_UPDATED",
                        "Project",
                        projectId,
                        details
                );
                System.out.println("📝 Project update logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log project update: " + logError.getMessage());
            }
        } else if (currentUser == null) {
            System.out.println("⚠️ WARNING: currentUser is null - project update logging skipped!");
        }

        return saved;
    }

    /**
 * Update VPN status for a project
 */
public Project updateVpnStatus(Long projectId, Project.VpnStatus vpnStatus, User currentUser) {
    Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

    Project.VpnStatus oldVpnStatus = project.getVpnStatus();
    
    // Update the VPN status
    project.setVpnStatus(vpnStatus);
    project.setUpdatedAt(java.time.LocalDateTime.now());
    
    Project saved = projectRepository.save(project);
    
    // Log the action if user is available
    if (currentUser != null && oldVpnStatus != vpnStatus) {
        String details = String.format("VPN status changed: %s → %s for project '%s' (ID: %d)",
                oldVpnStatus, vpnStatus, project.getProjectName(), projectId);
        
        try {
            activityLogService.logAction(
                    currentUser,
                    "PROJECT_VPN_STATUS_UPDATED",
                    "Project",
                    projectId,
                    details
            );
            System.out.println("📝 VPN status update logged by: " + currentUser.getUsername());
        } catch (Exception logError) {
            System.err.println("⚠️ Failed to log VPN status update: " + logError.getMessage());
        }
    }
    
    return saved;
}


    /**
     * Delete project - LEGACY METHOD (for backward compatibility)
     */
    public void deleteProject(Long projectId) {
        User currentUser = getCurrentUser();
        deleteProjectWithUser(projectId, currentUser);
    }

    /**
     * Delete project WITH EXPLICIT CURRENT USER (for reliable logging)
     */
    public void deleteProjectWithUser(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        String projectName = project.getProjectName();

        projectRepository.deleteById(projectId);
        if (currentUser != null) {
            String details = String.format("Deleted project '%s' (ID: %d)", projectName, projectId);
            try {
                activityLogService.logAction(
                        currentUser,
                        "PROJECT_DELETED",
                        "Project",
                        projectId,
                        details
                );
                System.out.println("📝 Project deletion logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log project deletion: " + logError.getMessage());
            }
        } else {
            System.out.println("⚠️ WARNING: currentUser is null - project deletion logging skipped!");
        }
    }

    /**
     * Recalculate project completion based on milestones
     */
    public Project recalculateProjectCompletion(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        List<Milestone> milestones = project.getMilestones();
        if (milestones.isEmpty()) {
            return project;
        }

        int totalCompletion = milestones.stream()
                .mapToInt(m -> m.getCompletionPercentage() != null ? m.getCompletionPercentage() : 0)
                .sum();

        int averageCompletion = totalCompletion / milestones.size();
        int oldCompletion = project.getCompletionPercentage() != null ? project.getCompletionPercentage() : 0;

        project.setCompletionPercentage(averageCompletion);

        Project.RagStatus newRagStatus = calculateRagStatus(project);
        project.setRagStatus(newRagStatus);

        Project savedProject = projectRepository.save(project);
        User currentUser = getCurrentUser();
        if (currentUser != null && (oldCompletion != averageCompletion || project.getRagStatus() != newRagStatus)) {
            String details = String.format("Project completion recalculated: %d%% → %d%% | RAG: %s",
                    oldCompletion, averageCompletion, newRagStatus);
            activityLogService.logAction(
                    currentUser,
                    "PROJECT_COMPLETION_RECALCULATED",
                    "Project",
                    projectId,
                    details
            );
        }

        return savedProject;
    }

    @Transactional(readOnly = true)
    public Optional<Project> findProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }
  @Transactional(readOnly = true)
public Optional<Project> findProjectByIdWithDetails(Long projectId) {
    return projectRepository.findById(projectId);
}
    @Transactional(readOnly = true)
    public List<Project> findAllProjects() {
        return projectRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsByStatus(Project.ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Project> findCriticalProjects() {
        List<Project.RagStatus> criticalStatuses = List.of(
                Project.RagStatus.RED,
                Project.RagStatus.AMBER
        );
        return projectRepository.findCriticalProjects(criticalStatuses);
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsByManager(User manager) {
        return projectRepository.findByManager(manager);
    }@Transactional(readOnly = true)
public List<Project> findProjectsByVpnStatus(Project.VpnStatus vpnStatus) {
    return projectRepository.findByVpnStatus(vpnStatus);
}

@Transactional(readOnly = true)
public List<Project> findProjectsByInitiatorAndVpnStatus(User initiatedBy, Project.VpnStatus vpnStatus) {
    if (initiatedBy == null) return Collections.emptyList();
    return projectRepository.findByInitiatorIdAndVpnStatus(initiatedBy.getId(), vpnStatus);
}

    /**
     * Assign manager to project
     */
    public Project assignManager(Long projectId, User manager) {
        return assignManagerWithUser(projectId, manager, getCurrentUser());
    }

    /**
     * Assign manager to project WITH EXPLICIT CURRENT USER
     */
    public Project assignManagerWithUser(Long projectId, User manager, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User oldManager = project.getManager();
        project.setManager(manager);

        Project saved = projectRepository.save(project);
        if (currentUser != null) {
            String details = String.format("Manager changed: %s → %s for project '%s'",
                    oldManager != null ? oldManager.getFullName() : "Unassigned",
                    manager != null ? manager.getFullName() : "Unassigned",
                    project.getProjectName());

            activityLogService.logAction(
                    currentUser,
                    "PROJECT_MANAGER_ASSIGNED",
                    "Project",
                    projectId,
                    details
            );
        }

        return saved;
    }

    /**
     * Update project status
     */
    public Project updateProjectStatus(Long projectId, Project.ProjectStatus status) {
        return updateProjectStatusWithUser(projectId, status, getCurrentUser());
    }

    /**
     * Update project status WITH EXPLICIT CURRENT USER
     */
    public Project updateProjectStatusWithUser(Long projectId, Project.ProjectStatus status, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Project.ProjectStatus oldStatus = project.getStatus();
        project.setStatus(status);

        if (status == Project.ProjectStatus.COMPLETED) {
            project.setActualEndDate(LocalDate.now());
        }
        Project.RagStatus newRagStatus = calculateRagStatus(project);
        project.setRagStatus(newRagStatus);

        Project saved = projectRepository.save(project);
        if (currentUser != null) {
            String details = String.format("Project status changed: %s → %s | RAG: %s",
                    oldStatus, status, newRagStatus);
            activityLogService.logAction(
                    currentUser,
                    "PROJECT_STATUS_UPDATED",
                    "Project",
                    projectId,
                    details
            );
        }

        return saved;
    }

    /**
     * Update RAG status (standalone call)
     */
    public Project updateRagStatus(Long projectId) {
        return updateRagStatusWithUser(projectId, getCurrentUser());
    }

    /**
     * Update RAG status WITH EXPLICIT CURRENT USER
     */
    public Project updateRagStatusWithUser(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Project.RagStatus oldRag = project.getRagStatus();
        Project.RagStatus newRag = calculateRagStatus(project);

        if (oldRag != newRag) {
            project.setRagStatus(newRag);
            Project saved = projectRepository.save(project);

            if (currentUser != null) {
                String details = String.format("RAG status updated: %s → %s (%s)",
                        oldRag, newRag, project.getProjectName());
                activityLogService.logAction(
                        currentUser,
                        "PROJECT_RAG_UPDATED",
                        "Project",
                        projectId,
                        details
                );
            }

            return saved;
        }

        return project;
    }

    @Transactional(readOnly = true)
    public List<Project> findActiveProjects() {
        List<Project.ProjectStatus> activeStatuses = List.of(
                Project.ProjectStatus.PLANNED,
                Project.ProjectStatus.IN_PROGRESS
        );
        return projectRepository.findActiveProjects(activeStatuses);
    }

    @Transactional(readOnly = true)
    public List<Project> findProjectsByDateRange(LocalDate startDate, LocalDate endDate) {
        System.out.println("=== DATE FILTER DEBUG ===");
        System.out.println("Start Date: " + startDate);
        System.out.println("End Date: " + endDate);

        if (startDate == null || endDate == null) {
            return findAllProjects();
        }

        List<Project> projects = projectRepository.findProjectsByDateRange(startDate, endDate);

        System.out.println("Filtered Projects Count: " + projects.size());

        return projects;
    }
    @Transactional(readOnly = true)
    public List<Attachment> findProjectAttachments(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        return attachmentRepository.findByProjectId(projectId);
    }

    /**
     * Calculate RAG status based on milestones and deadlines
     */
    
private Project.RagStatus calculateRagStatus(Project project) {
    if (project.getStatus() == Project.ProjectStatus.COMPLETED) {
        return Project.RagStatus.GREEN;
    }
    
    LocalDate today = LocalDate.now();
    LocalDate endDate = project.getEndDate();
    
    if (endDate == null) {
        return project.getRagStatus() != null ? project.getRagStatus() : Project.RagStatus.GREEN;
    }
    
    long daysUntilEnd = ChronoUnit.DAYS.between(today, endDate);
    
    if (daysUntilEnd < 0) {
        return Project.RagStatus.RED;
    }
    
    if (daysUntilEnd <= 7) {
        int completion = project.getCompletionPercentage() != null ? project.getCompletionPercentage() : 0;
        if (completion < 80) {
            return Project.RagStatus.AMBER;
        }
    }
    
    return Project.RagStatus.GREEN;
}
    
}