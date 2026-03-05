
package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.ProgressUpdate;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.ProgressUpdateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

@Service
@Transactional
public class ProgressUpdateService {

    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;

    @Autowired
    private ActivityLogService activityLogService;
     
    @Autowired
private JdbcTemplate jdbcTemplate;

    /**
     * Get current authenticated user from security context
     */
    private User getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
        } catch (Exception e) {
            System.err.println("Error getting current user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Check if user role is technical staff
     */
    private boolean isTechnicalRole(String role) {
        if (role == null) return false;
        return role.equals("DEVELOPER") || 
               role.equals("SENIOR_IT_OFFICER") || 
               role.equals("JUNIOR_IT_OFFICER") ||
               role.equals("IT_GRADUATE_TRAINEE");
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findAll() {
        return progressUpdateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ProgressUpdate> findById(Long id) {
        return progressUpdateRepository.findById(id);
    }
    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByUser(User user) {
        return progressUpdateRepository.findByUser(user);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByUserId(Long userId) {
        return progressUpdateRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByProject(Project project) {
        return progressUpdateRepository.findByProject(project);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByProjectId(Long projectId) {
        return progressUpdateRepository.findByProjectId(projectId);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByUserAndProject(User user, Project project) {
        return progressUpdateRepository.findByUserAndProject(user, project);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return progressUpdateRepository.findByUpdateDateBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findUpdatesWithBlockers() {
        return progressUpdateRepository.findUpdatesWithBlockers();
    }
@Transactional(readOnly = true)
public long countActiveBlockers() {
    return progressUpdateRepository.countActiveBlockers(); 
}

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findUserProgressHistory(User user, LocalDate startDate, LocalDate endDate) {
        return progressUpdateRepository.findByUserAndUpdateDateBetween(user, startDate, endDate);
    }
    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedBy(User submittedBy) {
        return progressUpdateRepository.findBySubmittedBy(submittedBy);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedById(Long submittedById) {
        return progressUpdateRepository.findBySubmittedById(submittedById);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedToManagementTrue() {
        return progressUpdateRepository.findBySubmittedToManagementTrue();
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedByIdAndSubmittedToManagementTrue(Long submittedById) {
        return progressUpdateRepository.findBySubmittedByIdAndSubmittedToManagementTrue(submittedById);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedByAndProject(User submittedBy, Project project) {
        return progressUpdateRepository.findBySubmittedByAndProject(submittedBy, project);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedByIdAndProjectId(Long submittedById, Long projectId) {
        return progressUpdateRepository.findBySubmittedByIdAndProjectId(submittedById, projectId);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findBySubmittedByIdAndUpdateDateBetween(Long submittedById, LocalDate startDate, LocalDate endDate) {
        return progressUpdateRepository.findBySubmittedByIdAndUpdateDateBetween(submittedById, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findUpdatesWithBlockersBySubmittedById(Long submittedById) {
        return progressUpdateRepository.findUpdatesWithBlockersBySubmittedById(submittedById);
    }
    @Transactional(readOnly = true)
    public long countBySubmittedById(Long submittedById) {
        return progressUpdateRepository.countBySubmittedById(submittedById);
    }

    @Transactional(readOnly = true)
    public long countBySubmittedByIdAndSubmittedToManagementTrue(Long submittedById) {
        return progressUpdateRepository.countBySubmittedByIdAndSubmittedToManagementTrue(submittedById);
    }
    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByProjectIdAndDateRange(Long projectId, LocalDate startDate, LocalDate endDate) {
        return progressUpdateRepository.findUpdatesByProjectAndDateRange(projectId, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public List<ProgressUpdate> findByUserIdAndDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        return progressUpdateRepository.findByUserIdAndUpdateDateBetween(userId, startDate, endDate);
    }

    /**
     * Create a new progress update - Technical staff submit to management
     */
    public ProgressUpdate createProgressUpdate(ProgressUpdate update, Project project, User submittedBy) {
        System.out.println("=== CREATING PROGRESS UPDATE ===");
        System.out.println("Project: " + project.getProjectName());
        System.out.println("Submitted by: " + (submittedBy != null ? submittedBy.getUsername() : "null"));
        System.out.println("Submitter role: " + (submittedBy != null && submittedBy.getRole() != null ? submittedBy.getRole().name() : "null"));
        
       
        update.setProject(project);
        update.setSubmittedBy(submittedBy);
        if (update.getUser() == null) {
            update.setUser(submittedBy);
        }
        if (update.getUpdateDate() == null) {
            update.setUpdateDate(LocalDate.now());
        }
        if (update.getSubmittedToManagement() == null) {
            String role = (submittedBy != null && submittedBy.getRole() != null) 
                ? submittedBy.getRole().name() 
                : "";
            update.setSubmittedToManagement(isTechnicalRole(role));
            System.out.println("submittedToManagement auto-set to: " + update.getSubmittedToManagement());
        }

        ProgressUpdate savedUpdate = progressUpdateRepository.save(update);
        System.out.println("✅ Progress update saved with ID: " + savedUpdate.getId());
        User currentUser = getCurrentUser();
        if (currentUser != null && savedUpdate.getSubmittedBy() != null) {
            String submitterName = savedUpdate.getSubmittedBy().getFullName();
            String submitterRole = savedUpdate.getSubmittedBy().getRole() != null 
                ? savedUpdate.getSubmittedBy().getRole().name() 
                : "Unknown";
            
            String details = String.format("Progress update submitted for project '%s' by %s (%s) - Completed: %s, Ongoing: %s%s",
                    project.getProjectName(),
                    submitterName,
                    submitterRole,
                    savedUpdate.getCompletedWork() != null && !savedUpdate.getCompletedWork().isEmpty() 
                        ? savedUpdate.getCompletedWork().substring(0, Math.min(50, savedUpdate.getCompletedWork().length())) + "..." 
                        : "N/A",
                    savedUpdate.getOngoingWork() != null && !savedUpdate.getOngoingWork().isEmpty()
                        ? savedUpdate.getOngoingWork().substring(0, Math.min(50, savedUpdate.getOngoingWork().length())) + "..."
                        : "N/A",
                    savedUpdate.getBlockers() != null && !savedUpdate.getBlockers().isBlank() 
                        ? " | ⚠ Blockers: " + savedUpdate.getBlockers().substring(0, Math.min(30, savedUpdate.getBlockers().length())) + "..." 
                        : ""
            );

            activityLogService.logAction(
                    currentUser,
                    savedUpdate.getSubmittedToManagement() ? "PROGRESS_UPDATE_SUBMITTED_TO_MANAGEMENT" : "PROGRESS_UPDATE_CREATED",
                    "Project",
                    project.getId(),
                    details
            );
            System.out.println("📝 Activity logged: " + (savedUpdate.getSubmittedToManagement() ? "SUBMITTED_TO_MANAGEMENT" : "CREATED"));
        }

        return savedUpdate;
    }
    public ProgressUpdate updateBlockerStatus(Long id, Map<String, Object> request, Long currentUserId) {
    ProgressUpdate update = progressUpdateRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Progress update not found: " + id));
    
    String blockerStatus = (String) request.get("blockerStatus");
    Long solvedById = request.get("solvedById") != null ? 
        Long.valueOf(request.get("solvedById").toString()) : null;
    String solvedByRole = (String) request.get("solvedByRole");
    
    LocalDateTime solvedAt = null;
    if (request.get("solvedAt") != null) {
        try {
            solvedAt = LocalDateTime.parse(request.get("solvedAt").toString());
        } catch (Exception e) {
            System.out.println("Error parsing solvedAt date: " + e.getMessage());
        }
    }
    
    update.setBlockerStatus(blockerStatus);
    update.setSolvedById(solvedById);
    update.setSolvedByRole(solvedByRole);
    update.setSolvedAt(solvedAt);

    return progressUpdateRepository.save(update);
}
    /**
     * Update an existing progress update
     */
    public ProgressUpdate updateProgressUpdate(Long id, ProgressUpdate updateDetails) {
        ProgressUpdate existingUpdate = progressUpdateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Progress update not found with ID: " + id));

        Project project = existingUpdate.getProject();
        User updater = existingUpdate.getSubmittedBy() != null ? existingUpdate.getSubmittedBy() : existingUpdate.getUser();

        StringBuilder changeLog = new StringBuilder();

        if (updateDetails.getCompletedWork() != null &&
                !updateDetails.getCompletedWork().equals(existingUpdate.getCompletedWork())) {
            changeLog.append(String.format("Completed work updated; "));
            existingUpdate.setCompletedWork(updateDetails.getCompletedWork());
        }

        if (updateDetails.getOngoingWork() != null &&
                !updateDetails.getOngoingWork().equals(existingUpdate.getOngoingWork())) {
            changeLog.append("Ongoing work updated; ");
            existingUpdate.setOngoingWork(updateDetails.getOngoingWork());
        }

        if (updateDetails.getBlockers() != null &&
                !updateDetails.getBlockers().equals(existingUpdate.getBlockers())) {
            changeLog.append("Blockers updated; ");
            existingUpdate.setBlockers(updateDetails.getBlockers());
        }

        if (updateDetails.getEstimatedResolution() != null &&
                !updateDetails.getEstimatedResolution().equals(existingUpdate.getEstimatedResolution())) {
            changeLog.append("Estimated resolution updated; ");
            existingUpdate.setEstimatedResolution(updateDetails.getEstimatedResolution());
        }

        ProgressUpdate savedUpdate = progressUpdateRepository.save(existingUpdate);
        if (changeLog.length() > 0 && updater != null) {
            User currentUser = getCurrentUser();
            if (currentUser != null) {
                String details = String.format("Updated progress for project '%s' by %s (%s): %s",
                        project.getProjectName(),
                        updater.getFullName(),
                        updater.getUsername(),
                        changeLog.toString().trim());

                activityLogService.logAction(
                        currentUser,
                        "PROGRESS_UPDATE_MODIFIED",
                        "Project",
                        project.getId(),
                        details
                );
            }
        }

        return savedUpdate;
    }

public void addComment(Long updateId, String comment, Long userId, String userRole) {
    String sql = """
        INSERT INTO progress_update_comments 
        (progress_update_id, comment, commenter_id, commenter_role, created_at) 
        VALUES (?, ?, ?, ?, NOW())
    """;
    
    jdbcTemplate.update(sql, updateId, comment, userId, userRole);
}

public List<Map<String, Object>> getComments(Long updateId) {
    String sql = """
        SELECT c.*, u.full_name as commenter_name 
        FROM progress_update_comments c
        LEFT JOIN users u ON c.commenter_id = u.id
        WHERE c.progress_update_id = ?
        ORDER BY c.created_at ASC
    """;
    
    return jdbcTemplate.queryForList(sql, updateId);
}


public void acknowledgeUpdate(Long updateId, Long userId) {
    String sql = """
        UPDATE progress_updates 
        SET acknowledged = TRUE, 
            acknowledged_by = ?, 
            acknowledged_at = NOW()
        WHERE id = ?
    """;
    
    jdbcTemplate.update(sql, userId, updateId);
}
    /**
     * Delete a progress update
     */
    public void deleteProgressUpdate(Long id) {
        ProgressUpdate update = progressUpdateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Progress update not found with ID: " + id));

        Project project = update.getProject();
        User updater = update.getSubmittedBy() != null ? update.getSubmittedBy() : update.getUser();

        progressUpdateRepository.deleteById(id);

        User currentUser = getCurrentUser();
        if (currentUser != null && updater != null) {
            String details = String.format("Deleted progress update for project '%s' by %s (originally reported %s)",
                    project.getProjectName(),
                    updater.getFullName(),
                    update.getUpdateDate());

            activityLogService.logAction(
                    currentUser,
                    "PROGRESS_UPDATE_DELETED",
                    "Project",
                    project.getId(),
                    details
            );
        }
    }
}