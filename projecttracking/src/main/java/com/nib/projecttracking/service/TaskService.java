package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.TaskRepository;
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
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

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


    @Transactional(readOnly = true)
    public List<Task> findAllTasks() {
        System.out.println("📦 Fetching all tasks with relationships...");
        return taskRepository.findAllWithRelationships();
    }

    @Transactional(readOnly = true)
    public Optional<Task> findTaskById(Long id) {
        if (id == null) return Optional.empty();
        return taskRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByProject(Project project) {
        if (project == null || project.getId() == null) return Collections.emptyList();
        return taskRepository.findByProject(project);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByAssignedUser(User user) {
        if (user == null || user.getId() == null) return Collections.emptyList();
        return taskRepository.findByAssignedTo(user);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByAssignedUserId(Long userId) {
        if (userId == null) return Collections.emptyList();
        System.out.println("📦 Fetching tasks for user " + userId + " with relationships...");
        return taskRepository.findByAssignedToIdWithRelationships(userId);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByStatus(Task.TaskStatus status) {
        if (status == null) return Collections.emptyList();
        return taskRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Task> findOverdueTasks() {
        LocalDate now = LocalDate.now();
        List<Task> allTasks = taskRepository.findAll();

        return allTasks.stream()
                .filter(t -> t != null && 
                             t.getDueDate() != null &&
                             t.getDueDate().isBefore(now) &&
                             t.getStatus() != Task.TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countOverdueTasks() {
        return findOverdueTasks().size();
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksDueThisWeek() {
        LocalDate today = LocalDate.now();
        LocalDate endOfWeek = today.plusDays(7);
        return taskRepository.findTasksDueThisWeek(today, endOfWeek);
    }

    @Transactional(readOnly = true)
    public List<Task> findAllTasksWithRelationships() {
        return taskRepository.findAllWithRelationships();
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByUserIdWithRelationships(Long userId) {
        if (userId == null) return Collections.emptyList();
        return taskRepository.findByAssignedToIdWithRelationships(userId);
    }

    @Transactional(readOnly = true)
    public List<Task> findTasksByProjectIdWithRelationships(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        return taskRepository.findByProjectIdWithRelationships(projectId);
    }
    @Transactional(readOnly = true)
    public List<Task> findTasksByProjectId(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        return taskRepository.findByProjectId(projectId);
    }

    /**
     * Create a new task - LEGACY METHOD (for backward compatibility)
     */
    public Task createTask(Task task, Project project, User assignedTo, User assignedBy) {
        User currentUser = getCurrentUser(); 
        return createTaskWithUser(task, project, assignedTo, assignedBy, currentUser);
    }

    /**
     * Create a new task WITH EXPLICIT CURRENT USER (for reliable logging)
     */
    public Task createTaskWithUser(Task task, Project project, User assignedTo, User assignedBy, User currentUser) {
        if (task == null) throw new IllegalArgumentException("Task cannot be null");

        System.out.println("=== TASK SERVICE - CREATE ===");
        System.out.println("Task name: " + task.getTaskName());
        System.out.println("Project: " + (project != null ? project.getProjectName() : "null"));
        System.out.println("Assigned to: " + (assignedTo != null ? assignedTo.getUsername() : "null"));
        System.out.println("Current user (for logging): " + (currentUser != null ? currentUser.getUsername() : "null"));

        task.setProject(project);
        task.setAssignedTo(assignedTo);

        if (assignedBy != null) {
            task.setAssignedBy(assignedBy);
        }

        if (task.getStatus() == null) {
            task.setStatus(Task.TaskStatus.PENDING);
        }

        if (task.getPriority() == null) {
            task.setPriority(2); 
        }

        Task savedTask = taskRepository.save(task);
        System.out.println("Task saved with ID: " + savedTask.getId());

       
        if (currentUser != null) {
            String details = String.format("Created task '%s' in project '%s', assigned to %s by %s",
                    savedTask.getTaskName(),
                    project != null ? project.getProjectName() : "N/A",
                    assignedTo != null ? assignedTo.getFullName() : "Unassigned",
                    assignedBy != null ? assignedBy.getFullName() : "System");

            try {
                activityLogService.logAction(
                        currentUser,
                        "TASK_CREATED",
                        "Task",
                        savedTask.getId(),
                        details
                );
                System.out.println("📝 Task creation logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log task creation: " + logError.getMessage());
              
            }
        } else {
            System.out.println("⚠️ WARNING: currentUser is null - task logging skipped!");
        }

        if (project != null && project.getId() != null) {
            updateProjectCompletion(project.getId());
        }

        return savedTask;
    }


    /**
     * Update task status - LEGACY METHOD (for backward compatibility)
     */
    public Task updateTaskStatus(Long taskId, Task.TaskStatus newStatus) {
        User currentUser = getCurrentUser();
        return updateTaskStatusWithUser(taskId, newStatus, currentUser);
    }

    /**
     * Update task status WITH EXPLICIT CURRENT USER
     */
    public Task updateTaskStatusWithUser(Long taskId, Task.TaskStatus newStatus, User currentUser) {
        if (taskId == null || newStatus == null) {
            throw new IllegalArgumentException("taskId and newStatus cannot be null");
        }

        System.out.println("=== TASK SERVICE - UPDATE STATUS ===");
        System.out.println("Task ID: " + taskId + ", New status: " + newStatus);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        Task.TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == Task.TaskStatus.COMPLETED) {
            task.setCompletedDate(LocalDate.now());
        }

        Task updatedTask = taskRepository.save(task);

        if (currentUser != null) {
            String details = String.format("Task status changed: %s → %s ('%s')",
                    oldStatus, newStatus, task.getTaskName());

            try {
                activityLogService.logAction(
                        currentUser,
                        "TASK_STATUS_UPDATED",
                        "Task",
                        taskId,
                        details
                );
                System.out.println("📝 Task status update logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log status update: " + logError.getMessage());
            }
        }

        if (task.getProject() != null && task.getProject().getId() != null) {
            updateProjectCompletion(task.getProject().getId());
        }

        return updatedTask;
    }

    /**
     * Update task details - LEGACY METHOD (for backward compatibility)
     */
    public Task updateTask(Long taskId, Task taskDetails) {
        User currentUser = getCurrentUser();
        return updateTaskWithUser(taskId, taskDetails, currentUser);
    }

    /**
     * Update task details WITH EXPLICIT CURRENT USER
     */
    public Task updateTaskWithUser(Long taskId, Task taskDetails, User currentUser) {
        if (taskId == null || taskDetails == null) {
            throw new IllegalArgumentException("taskId and taskDetails cannot be null");
        }

        System.out.println("=== TASK SERVICE - UPDATE ===");
        System.out.println("Task ID: " + taskId);

        Task existingTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        StringBuilder changes = new StringBuilder();

        if (taskDetails.getTaskName() != null && !taskDetails.getTaskName().equals(existingTask.getTaskName())) {
            changes.append("Name updated; ");
            existingTask.setTaskName(taskDetails.getTaskName());
        }

        if (taskDetails.getDescription() != null && !taskDetails.getDescription().equals(existingTask.getDescription())) {
            changes.append("Description updated; ");
            existingTask.setDescription(taskDetails.getDescription());
        }

        if (taskDetails.getPriority() != null && !taskDetails.getPriority().equals(existingTask.getPriority())) {
            changes.append("Priority updated; ");
            existingTask.setPriority(taskDetails.getPriority());
        }

        if (taskDetails.getDueDate() != null && !taskDetails.getDueDate().equals(existingTask.getDueDate())) {
            changes.append("Due date updated; ");
            existingTask.setDueDate(taskDetails.getDueDate());
        }

        Task updatedTask = taskRepository.save(existingTask);

        if (changes.length() > 0 && currentUser != null) {
            String details = "Task updated: " + changes.toString().trim() + " ('" + existingTask.getTaskName() + "')";
            try {
                activityLogService.logAction(
                        currentUser,
                        "TASK_UPDATED",
                        "Task",
                        taskId,
                        details
                );
                System.out.println("📝 Task update logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log task update: " + logError.getMessage());
            }
        } else if (currentUser == null && changes.length() > 0) {
            System.out.println("⚠️ WARNING: currentUser is null - task update logging skipped!");
        }

        return updatedTask;
    }

    /**
     * Delete task - LEGACY METHOD (for backward compatibility)
     */
    public void deleteTask(Long taskId) {
        User currentUser = getCurrentUser();
        deleteTaskWithUser(taskId, currentUser);
    }

    /**
     * Delete task WITH EXPLICIT CURRENT USER
     */
    public void deleteTaskWithUser(Long taskId, User currentUser) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }

        System.out.println("=== TASK SERVICE - DELETE ===");
        System.out.println("Task ID: " + taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        Project project = task.getProject();
        String taskName = task.getTaskName();

        taskRepository.deleteById(taskId);

        if (currentUser != null) {
            String details = String.format("Deleted task '%s' from project '%s'",
                    taskName,
                    project != null ? project.getProjectName() : "N/A");

            try {
                activityLogService.logAction(
                        currentUser,
                        "TASK_DELETED",
                        "Task",
                        taskId,
                        details
                );
                System.out.println("📝 Task deletion logged by: " + currentUser.getUsername());
            } catch (Exception logError) {
                System.err.println("⚠️ Failed to log task deletion: " + logError.getMessage());
            }
        } else {
            System.out.println("⚠️ WARNING: currentUser is null - task deletion logging skipped!");
        }
        if (project != null && project.getId() != null) {
            updateProjectCompletion(project.getId());
        }
    }


    @Transactional(readOnly = true)
    public long countTasksByUser(User user) {
        if (user == null || user.getId() == null) return 0L;
        return taskRepository.countByAssignedTo(user);
    }

    @Transactional(readOnly = true)
    public long countCompletedTasksByUser(User user) {
        if (user == null || user.getId() == null) return 0L;
        return taskRepository.countByAssignedToAndStatus(user, Task.TaskStatus.COMPLETED);
    }

    @Transactional
    public void updateProjectCompletion(Long projectId) {
        if (projectId == null) {
            System.err.println("Cannot update completion: projectId is null");
            return;
        }

        System.out.println("=== UPDATING PROJECT COMPLETION ===");
        System.out.println("Project ID: " + projectId);

        try {
            Optional<Project> projectOpt = projectRepository.findById(projectId);
            if (projectOpt.isEmpty()) {
                System.err.println("Project not found with ID: " + projectId);
                return;
            }
            Project project = projectOpt.get();

            List<Task> allTasks = taskRepository.findByProject(project);

            int completionPercentage = 0;
            if (!allTasks.isEmpty()) {
                long completedTasks = allTasks.stream()
                        .filter(t -> t != null && t.getStatus() == Task.TaskStatus.COMPLETED)
                        .count();
                completionPercentage = (int) ((completedTasks * 100L) / allTasks.size());
            }

            project.setCompletionPercentage(completionPercentage);

            if (completionPercentage == 100) {
                project.setStatus(Project.ProjectStatus.COMPLETED);
            } else if (completionPercentage > 0) {
                project.setStatus(Project.ProjectStatus.IN_PROGRESS);
            } else {
                project.setStatus(Project.ProjectStatus.PLANNED);
            }

            projectRepository.save(project);

            User currentUser = getCurrentUser();
            if (currentUser != null) {
                activityLogService.logAction(
                        currentUser,
                        "PROJECT_COMPLETION_UPDATED_FROM_TASK",
                        "Project",
                        projectId,
                        String.format("Project completion recalculated to %d%% based on %d tasks",
                                completionPercentage, allTasks.size())
                );
            }

            System.out.println("✅ Project completion updated to " + completionPercentage + "%");

        } catch (Exception e) {
            System.err.println("❌ Error updating project completion for ID " + projectId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public void recalculateProjectCompletion(Long projectId) {
        updateProjectCompletion(projectId);
    }

    @Transactional
    public void recalculateAllProjectCompletions() {
        System.out.println("=== RECALCULATING ALL PROJECT COMPLETIONS ===");
        List<Project> allProjects = projectRepository.findAll();

        for (Project project : allProjects) {
            if (project == null || project.getId() == null) continue;
            try {
                updateProjectCompletion(project.getId());
            } catch (Exception e) {
                System.err.println("Error updating project " + project.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ All project completions recalculated");
    }
}