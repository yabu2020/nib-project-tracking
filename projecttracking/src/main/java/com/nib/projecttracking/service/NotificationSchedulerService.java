package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.UserNotification;
import com.nib.projecttracking.repository.UserNotificationRepository;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotificationSchedulerService {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private MilestoneService milestoneService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private UserNotificationRepository userNotificationRepository;
   
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    
    /**
     * Daily check for overdue tasks - runs at 8:00 AM
     */
    
@Scheduled(cron = "0 0 8,20 * * *")
public void verifySchedulingActive() {
    System.out.println("⏰ [" + java.time.LocalDateTime.now() + "] Scheduling is ACTIVE!");
}
  @Scheduled(cron = "0 0 8,20 * * *")
@Transactional
public void checkAndNotifyOverdueTasks() {
    System.out.println("\n🔄 Running overdue task notification check...");
    System.out.println("Current time: " + java.time.LocalDateTime.now());
    
    try {
        // Get ALL tasks first to see what's in the database
        List<Task> allTasks = taskService.findAllTasks(); // Add this method if it doesn't exist
        System.out.println("📊 Total tasks in system: " + allTasks.size());
        
        for (Task task : allTasks) {
            System.out.println("  Task: " + task.getTaskName());
            System.out.println("    Due Date: " + task.getDueDate());
            System.out.println("    Status: " + task.getStatus());
            System.out.println("    Assigned To: " + (task.getAssignedTo() != null ? task.getAssignedTo().getUsername() : "NULL"));
            System.out.println("    Is Overdue: " + (task.getDueDate() != null && task.getDueDate().isBefore(java.time.LocalDate.now())));
        }
        
        // Now get overdue tasks
        List<Task> overdueTasks = taskService.findOverdueTasks();
        System.out.println("✅ Found " + overdueTasks.size() + " overdue tasks");
        
        if (overdueTasks.isEmpty()) {
            System.out.println("⚠️ NO OVERDUE TASKS FOUND - Check task dates and status!");
        }
        
        for (Task task : overdueTasks) {
            System.out.println("Processing overdue task: " + task.getTaskName());
            User assignedTo = task.getAssignedTo();
            
            if (assignedTo != null) {
                String dueDate = task.getDueDate() != null ? 
                    task.getDueDate().format(DATE_FORMAT) : "Not set";
                
                createInAppNotification(
                    assignedTo,
                    "TASK_OVERDUE",
                    "⚠️ Task Overdue: " + task.getTaskName(),
                    "Your task '" + task.getTaskName() + "' in project '" + 
                    (task.getProject() != null ? task.getProject().getProjectName() : "Unknown") + 
                    "' was due on " + dueDate,
                    "HIGH",
                    "Task",
                    task.getId()
                );
            } else {
                System.out.println("⚠️ Task has no assigned user: " + task.getTaskName());
            }
        }
        
        System.out.println("✅ Overdue task notification check completed");
        
    } catch (Exception e) {
        System.err.println("❌ Error in overdue task notification: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    /**
     * Daily check for overdue milestones - runs at 8:05 AM
     */
 @Scheduled(cron = "0 5 8,20 * * *")
    @Transactional
    public void checkAndNotifyOverdueMilestones() {
        System.out.println("\n🔄 Running overdue milestone notification check...");
        
        try {
            List<Milestone> overdueMilestones = milestoneService.findOverdueMilestones();
            System.out.println("Found " + overdueMilestones.size() + " overdue milestones");
            
            for (Milestone milestone : overdueMilestones) {
                if (milestone.getProject() != null && milestone.getProject().getManager() != null) {
                    
                    User manager = milestone.getProject().getManager();
                    String dueDate = milestone.getTargetDate() != null ? 
                        milestone.getTargetDate().format(DATE_FORMAT) : "Not set";
                    
                   
                    createInAppNotification(
                        manager,
                        "MILESTONE_OVERDUE",
                        "🎯 Milestone Overdue: " + milestone.getMilestoneName(),
                        "The milestone '" + milestone.getMilestoneName() + "' for project '" + 
                        milestone.getProject().getProjectName() + "' was due on " + dueDate,
                        "URGENT",
                        "Milestone",
                        milestone.getId()
                    );
                    
                    User initiatedBy = milestone.getProject().getInitiatedBy();
                    if (initiatedBy != null && !initiatedBy.getId().equals(manager.getId())) {
                        createInAppNotification(
                            initiatedBy,
                            "MILESTONE_OVERDUE",
                            "🎯 Milestone Overdue: " + milestone.getMilestoneName(),
                            "The milestone '" + milestone.getMilestoneName() + "' for project '" + 
                            milestone.getProject().getProjectName() + "' was due on " + dueDate,
                            "HIGH",
                            "Milestone",
                            milestone.getId()
                        );
                    }
                  
                    if (manager.getEmail() != null && !manager.getEmail().isEmpty()) {
                        System.out.println("📧 Sending overdue milestone alert to: " + manager.getEmail());
                        
                        emailService.sendOverdueMilestoneAlert(
                            manager.getEmail(),
                            manager.getId(),
                            manager.getFullName() != null ? manager.getFullName() : manager.getUsername(),
                            milestone.getMilestoneName(),
                            milestone.getProject().getProjectName(),
                            dueDate
                        );
                    }
                }
            }
            
            System.out.println("✅ Overdue milestone notification check completed");
            
        } catch (Exception e) {
            System.err.println("❌ Error in overdue milestone notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Transactional
    private void createInAppNotification(User user, String type, String title, 
                                         String message, String priority, 
                                         String entityType, Long entityId) {
        try {
           
            boolean exists = userNotificationRepository.existsByUserIdAndTypeAndRelatedEntityId(
                user.getId(), type, entityId 
            );
            
            if (!exists) {
                UserNotification notification = new UserNotification();
                notification.setUser(user);
                notification.setType(type);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setPriority(priority);
                notification.setRead(false);
                notification.setRelatedEntityType(entityType);
                notification.setRelatedEntityId(entityId);
                
                userNotificationRepository.save(notification);
                System.out.println("✅ Created in-app notification for user: " + user.getUsername() + 
                                 " | Type: " + type + " | Entity: " + entityId);
            } else {
                System.out.println("⚠️ Notification already exists for user: " + user.getUsername() + 
                                 " | Type: " + type + " | Entity: " + entityId);
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to create notification: " + e.getMessage());
        }
    }
    
    /**
     * Manual trigger for testing
     */
    @Transactional
    public void triggerNotificationsManually() {
        System.out.println("🔔 Manually triggering notifications...");
        checkAndNotifyOverdueTasks();
        checkAndNotifyOverdueMilestones();
    }
}