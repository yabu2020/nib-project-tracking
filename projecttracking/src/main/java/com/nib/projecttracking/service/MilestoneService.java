package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.MilestoneRepository;
import com.nib.projecttracking.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MilestoneService {

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ActivityLogService activityLogService;  

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        return null;
    }
    


    /**
     * Create milestone (FR-01: Project Baseline Definition)
     */
   public Milestone createMilestone(Milestone milestone, Project project) {

   
    Project existingProject = projectRepository.findById(project.getId())
            .orElseThrow(() -> new RuntimeException("Project not found"));

    milestone.setProject(existingProject);
    milestone.setStatus(Milestone.MilestoneStatus.PLANNED);

    if (milestone.getCompletionPercentage() == null) {
        milestone.setCompletionPercentage(0);
    }

    Milestone savedMilestone = milestoneRepository.save(milestone);

    updateProjectCompletion(existingProject); 
    return savedMilestone;
}

    /**
     * Find milestone by ID
     */
    @Transactional(readOnly = true)
    public Optional<Milestone> findMilestoneById(Long milestoneId) {
        return milestoneRepository.findById(milestoneId);
    }
    /**
 * Find milestones by project ID
 */
@Transactional(readOnly = true)
public List<Milestone> findMilestonesByProjectId(Long projectId) {
    if (projectId == null) return Collections.emptyList();
    return milestoneRepository.findByProjectId(projectId);
}

    /**
     * Find all milestones
     */
    @Transactional(readOnly = true)
    public List<Milestone> findAllMilestones() {
        return milestoneRepository.findAll();
    }

    /**
     * Find milestones by project
     */
    @Transactional(readOnly = true)
    public List<Milestone> findMilestonesByProject(Project project) {
        return milestoneRepository.findByProject(project);
    }

    /**
     * Find milestones by project ID (for detailed report)
     */
    @Transactional(readOnly = true)
    public List<Milestone> findByProjectId(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        return milestoneRepository.findByProjectId(projectId);
    }

    /**
     * Find milestones by status
     */
    @Transactional(readOnly = true)
    public List<Milestone> findMilestonesByStatus(Milestone.MilestoneStatus status) {
        if (status == null) {
            return milestoneRepository.findAll();
        }
        return milestoneRepository.findByStatus(status);
    }

    /**
     * Find completed milestones
     */
    @Transactional(readOnly = true)
    public List<Milestone> findCompletedMilestones(Project project) {
        return milestoneRepository.findCompletedMilestonesByProject(project);
    }

    /**
     * Find overdue milestones (past OR today, and not completed/cancelled)
     */
    @Transactional(readOnly = true)
    public List<Milestone> findOverdueMilestones() {
        LocalDate now = LocalDate.now();
        List<Milestone> allMilestones = milestoneRepository.findAll();

        List<Milestone> overdue = allMilestones.stream()
                .filter(m -> m.getTargetDate() != null &&
                        !m.getTargetDate().isAfter(now) &&
                        m.getStatus() != Milestone.MilestoneStatus.COMPLETED &&
                        m.getStatus() != Milestone.MilestoneStatus.CANCELLED)
                .collect(Collectors.toList());

        return overdue;
    }

    /**
     * Count overdue milestones - FOR DASHBOARD
     */
    @Transactional(readOnly = true)
    public long countOverdueMilestones() {
        return findOverdueMilestones().size();
    }

    /**
     * Find upcoming milestones (next 30 days, not completed)
     */
    @Transactional(readOnly = true)
    public List<Milestone> findUpcomingMilestones() {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        return milestoneRepository.findAll().stream()
                .filter(m -> m.getTargetDate() != null &&
                        !m.getTargetDate().isBefore(today) &&
                        !m.getTargetDate().isAfter(thirtyDaysLater) &&
                        m.getStatus() != Milestone.MilestoneStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    /**
     * Count upcoming milestones - FOR DASHBOARD
     */
    @Transactional(readOnly = true)
    public long countUpcomingMilestones() {
        return findUpcomingMilestones().size();
    }


    /**
     * Update milestone status
     */
    public Milestone updateMilestoneStatus(Long milestoneId, Milestone.MilestoneStatus status) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        Milestone.MilestoneStatus oldStatus = milestone.getStatus();
        milestone.setStatus(status);

        if (status == Milestone.MilestoneStatus.COMPLETED) {
            milestone.setActualDate(LocalDate.now());
            milestone.setCompletionPercentage(100);
        }

        Milestone saved = milestoneRepository.save(milestone);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Changed milestone status '%s' → '%s' (%s)",
                    oldStatus, status, milestone.getMilestoneName());
            activityLogService.logAction(
                    currentUser,
                    "MILESTONE_STATUS_UPDATED",
                    "Milestone",
                    milestoneId,
                    details
            );
        }

        updateProjectCompletion(milestone.getProject());

        return saved;
    }

    /**
     * Update milestone progress
     */
    public Milestone updateMilestoneProgress(Long milestoneId, int completionPercentage) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        int oldPercentage = milestone.getCompletionPercentage() != null ? milestone.getCompletionPercentage() : 0;
        milestone.setCompletionPercentage(completionPercentage);

        if (completionPercentage >= 100) {
            milestone.setStatus(Milestone.MilestoneStatus.COMPLETED);
            milestone.setActualDate(LocalDate.now());
        } else if (completionPercentage > 0) {
            milestone.setStatus(Milestone.MilestoneStatus.IN_PROGRESS);
        } else {
            milestone.setStatus(Milestone.MilestoneStatus.PLANNED);
        }

        Milestone saved = milestoneRepository.save(milestone);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Updated milestone progress %d%% → %d%% (%s)",
                    oldPercentage, completionPercentage, milestone.getMilestoneName());
            activityLogService.logAction(
                    currentUser,
                    "MILESTONE_PROGRESS_UPDATED",
                    "Milestone",
                    milestoneId,
                    details
            );
        }

        updateProjectCompletion(milestone.getProject());

        return saved;
    }

    /**
     * Update milestone details
     */
    public Milestone updateMilestone(Long milestoneId, Milestone milestoneDetails) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        if (milestoneDetails.getMilestoneName() != null) {
            milestone.setMilestoneName(milestoneDetails.getMilestoneName());
        }
        if (milestoneDetails.getDescription() != null) {
            milestone.setDescription(milestoneDetails.getDescription());
        }
        if (milestoneDetails.getTargetDate() != null) {
            milestone.setTargetDate(milestoneDetails.getTargetDate());
        }
        if (milestoneDetails.getCompletionPercentage() != null) {
            milestone.setCompletionPercentage(milestoneDetails.getCompletionPercentage());
        }

        Milestone saved = milestoneRepository.save(milestone);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = "Updated milestone details: " + milestone.getMilestoneName();
            activityLogService.logAction(
                    currentUser,
                    "MILESTONE_UPDATED",
                    "Milestone",
                    milestoneId,
                    details
            );
        }

        updateProjectCompletion(milestone.getProject());

        return saved;
    }

    /**
     * Update project completion based on milestones
     */
    private void updateProjectCompletion(Project project) {
        if (project == null) return;

        List<Milestone> milestones = milestoneRepository.findByProject(project);

        if (milestones.isEmpty()) {
            project.setCompletionPercentage(0);
            projectRepository.save(project);
            return;
        }

        int totalCompletion = milestones.stream()
                .mapToInt(m -> m.getCompletionPercentage() != null ? m.getCompletionPercentage() : 0)
                .sum();

        int averageCompletion = totalCompletion / milestones.size();

        project.setCompletionPercentage(averageCompletion);
        projectRepository.save(project);
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            activityLogService.logAction(
                    currentUser,
                    "PROJECT_COMPLETION_UPDATED",
                    "Project",
                    project.getId(),
                    String.format("Project completion recalculated to %d%% based on %d milestones",
                            averageCompletion, milestones.size())
            );
        }
    }

    /**
     * Delete milestone
     */
    public void deleteMilestone(Long milestoneId) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found"));

        Project project = milestone.getProject();
        String milestoneName = milestone.getMilestoneName();

        milestoneRepository.deleteById(milestoneId);

        User currentUser = getCurrentUser();
        if (currentUser != null) {
            String details = String.format("Deleted milestone '%s' from project '%s'",
                    milestoneName, project != null ? project.getProjectName() : "N/A");
            activityLogService.logAction(
                    currentUser,
                    "MILESTONE_DELETED",
                    "Milestone",
                    milestoneId,
                    details
            );
        }
        if (project != null) {
            updateProjectCompletion(project);
        }
    }

    /**
     * Find milestones by date range (FR-06)
     */
    @Transactional(readOnly = true)
    public List<Milestone> findMilestonesByDateRange(LocalDate startDate, LocalDate endDate) {
        return milestoneRepository.findMilestonesByDateRange(startDate, endDate);
    }

    

    /**
     * Calculate project completion based on milestones
     */
    @Transactional(readOnly = true)
    public int calculateProjectCompletion(Project project) {
        List<Milestone> milestones = milestoneRepository.findByProject(project);
        if (milestones.isEmpty()) return 0;

        int totalWeight = milestones.size() * 100;
        int completedWeight = milestones.stream()
                .mapToInt(m -> m.getCompletionPercentage() != null ? m.getCompletionPercentage() : 0)
                .sum();

        return totalWeight > 0 ? (completedWeight * 100) / totalWeight : 0;
    }
}