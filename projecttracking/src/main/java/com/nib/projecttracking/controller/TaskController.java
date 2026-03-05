package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.service.TaskService;
import com.nib.projecttracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserService userService;
   
    
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        System.out.println("=== GET ALL TASKS ===");
        System.out.println("User ID: " + userId);
        System.out.println("User Role: " + userRole);
        
        List<Task> tasks;
        
        if (userId != null && userRole != null) {
            if (isManagerRole(userRole)) {
                System.out.println("Manager/Executive role - fetching all tasks WITH relationships");
                tasks = taskService.findAllTasks();
                System.out.println("Returning " + tasks.size() + " tasks with project/user data");
                return ResponseEntity.ok(tasks);
            }
            
            if (isTechnicalRole(userRole)) {
                System.out.println("Technical staff - fetching assigned tasks WITH relationships");
                tasks = taskService.findTasksByAssignedUserId(userId);
                System.out.println("Returning " + tasks.size() + " assigned tasks with project/user data");
                return ResponseEntity.ok(tasks);
            }
        }
        
        System.out.println("No user filter - fetching all tasks WITH relationships");
        tasks = taskService.findAllTasks();
        System.out.println("Returning " + tasks.size() + " tasks with project/user data");
        return ResponseEntity.ok(tasks);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {
        return taskService.findTaskById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Task>> getTasksByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return ResponseEntity.ok(taskService.findTasksByProject(project));
    }
    
    @GetMapping("/assigned-to/{userId}")
    public ResponseEntity<List<Task>> getTasksByAssignedUser(@PathVariable Long userId) {
        User user = new User();
        user.setId(userId);
        return ResponseEntity.ok(taskService.findTasksByAssignedUser(user));
    }
    
    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.findOverdueTasks());
    }
    
    @GetMapping("/due-this-week")
    public ResponseEntity<List<Task>> getTasksDueThisWeek() {
        return ResponseEntity.ok(taskService.findTasksDueThisWeek());
    }
    
    @GetMapping("/recalculate-all-completions")
    public ResponseEntity<?> recalculateAllCompletions() {
        taskService.recalculateAllProjectCompletions();
        return ResponseEntity.ok(Map.of("message", "All project completions recalculated"));
    }
    
    
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> taskData,
                                         @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            System.out.println("=== CREATE TASK REQUEST ===");
            System.out.println("🔍 Current User ID from header: " + currentUserId);
            System.out.println("Request  " + taskData);
            
            Task task = new Task();
            task.setTaskName((String) taskData.get("taskName"));
            task.setDescription((String) taskData.get("description"));
            
            
            if (taskData.get("priority") != null) {
                try {
                    int priority = Integer.parseInt(taskData.get("priority").toString());
                    if (priority >= 1 && priority <= 4) {
                        task.setPriority(priority);
                    } else {
                        task.setPriority(2);
                    }
                } catch (NumberFormatException e) {
                    task.setPriority(2);
                }
            }
            
           
            if (taskData.get("status") != null) {
                try {
                    Task.TaskStatus status = Task.TaskStatus.valueOf(
                        taskData.get("status").toString().toUpperCase()
                    );
                    task.setStatus(status);
                } catch (IllegalArgumentException e) {
                    task.setStatus(Task.TaskStatus.PENDING);
                }
            }
            
            if (taskData.get("dueDate") != null) {
                try {
                    task.setDueDate(LocalDate.parse(taskData.get("dueDate").toString()));
                } catch (Exception e) {
                    System.out.println("Due date parse error: " + e.getMessage());
                }
            }
            
           
            Project project = new Project();
            if (taskData.get("projectId") != null) {
                project.setId(Long.valueOf(taskData.get("projectId").toString()));
            }
            
           
            User assignedTo = new User();
            if (taskData.get("assignedTo") != null) {
                assignedTo.setId(Long.valueOf(taskData.get("assignedTo").toString()));
            }
            
         
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
                System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            }
            
        
            Task createdTask = taskService.createTaskWithUser(task, project, assignedTo, null, currentUser);
            
            System.out.println("Task created successfully with ID: " + createdTask.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Task created successfully",
                "task", createdTask
            ));
        } catch (Exception e) {
            System.err.println("Error creating task: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTaskStatus(@PathVariable Long id, 
                                               @RequestBody Map<String, String> data,
                                               @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            Task.TaskStatus status = Task.TaskStatus.valueOf(data.get("status").toUpperCase());
            
          
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
            }
            
            Task updatedTask = taskService.updateTaskStatusWithUser(id, status, currentUser);
            return ResponseEntity.ok(Map.of(
                "message", "Task status updated successfully",
                "task", updatedTask
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value: " + data.get("status")));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, 
                                         @RequestBody Map<String, Object> taskData,
                                         @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            System.out.println("=== UPDATE TASK REQUEST ===");
            System.out.println("🔍 Current User ID from header: " + currentUserId);
            
            Task taskDetails = new Task();
            taskDetails.setTaskName((String) taskData.get("taskName"));
            taskDetails.setDescription((String) taskData.get("description"));
            
            if (taskData.get("priority") != null) {
                try {
                    int priority = Integer.parseInt(taskData.get("priority").toString());
                    if (priority >= 1 && priority <= 4) {
                        taskDetails.setPriority(priority);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid priority format");
                }
            }
            
            if (taskData.get("dueDate") != null) {
                try {
                    taskDetails.setDueDate(LocalDate.parse(taskData.get("dueDate").toString()));
                } catch (Exception e) {
                    System.out.println("Due date parse error: " + e.getMessage());
                }
            }
            
           
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
                System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            }
            
           
            Task updatedTask = taskService.updateTaskWithUser(id, taskDetails, currentUser);
            
            return ResponseEntity.ok(Map.of(
                "message", "Task updated successfully",
                "task", updatedTask
            ));
        } catch (Exception e) {
            System.err.println("Error updating task: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
  
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                         @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            System.out.println("=== DELETE TASK REQUEST ===");
            System.out.println("🔍 Current User ID from header: " + currentUserId);
            
          
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
                System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            }
            
            
            taskService.deleteTaskWithUser(id, currentUser);
            
            return ResponseEntity.ok(Map.of("message", "Task deleted successfully"));
        } catch (Exception e) {
            System.err.println("Error deleting task: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    
    
    private boolean isTechnicalRole(String role) {
        if (role == null) return false;
        return role.equals("DEVELOPER") || 
               role.equals("SENIOR_IT_OFFICER") || 
               role.equals("JUNIOR_IT_OFFICER") ||
               role.equals("IT_GRADUATE_TRAINEE");
    }
    
    private boolean isManagerRole(String role) {
        if (role == null) return false;
        return role.equals("CEO") || 
               role.equals("DEPUTY_CHIEF") ||
               role.equals("DIRECTOR") ||
               role.equals("BUSINESS") ||
               role.equals("QUALITY_ASSURANCE") ||
               role.equals("PROJECT_MANAGER") ||
               role.equals("CORE_BANKING_MANAGER") ||
               role.equals("DIGITAL_BANKING_MANAGER");
    }
}