package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Api;
import com.nib.projecttracking.entity.Attachment;
import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.repository.ProjectRepository;
import com.nib.projecttracking.service.ActivityLogService;
import com.nib.projecttracking.service.ProjectService;
import com.nib.projecttracking.service.TaskService;
import com.nib.projecttracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nib.projecttracking.service.MilestoneService;
import com.nib.projecttracking.service.ApiService;
import com.nib.projecttracking.service.AttachmentService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ProjectController {
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
private MilestoneService milestoneService;

@Autowired
private ApiService apiService;

 @Autowired
    private AttachmentService attachmentService;
    
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        System.out.println("=== GET ALL PROJECTS ===");
        System.out.println("User ID: " + userId);
        System.out.println("User Role: " + userRole);
        
        List<Project> allProjects = projectService.findAllProjects();
        
        if (userId != null && userRole != null) {
            if (isBusinessRole(userRole)) {
                List<Project> userProjects = allProjects.stream()
                    .filter(p -> p.getInitiatedBy() != null && 
                                p.getInitiatedBy().getId().equals(userId))
                    .collect(Collectors.toList());
                
                System.out.println("📊 Business user - returning " + userProjects.size() + " own projects");
                return ResponseEntity.ok(userProjects);
            }
            
            if (isManagerRole(userRole)) {
                System.out.println("👔 Manager role - returning all " + allProjects.size() + " projects");
                return ResponseEntity.ok(allProjects);
            }
     
            if (isTechnicalRole(userRole)) {
                User user = new User();
                user.setId(userId);
                List<Task> userTasks = taskService.findTasksByAssignedUser(user);
                
                List<Project> userProjects = userTasks.stream()
                    .map(Task::getProject)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
                
                System.out.println("🔧 Technical staff - returning " + userProjects.size() + " assigned projects");
                return ResponseEntity.ok(userProjects);
            }
        }
        
        System.out.println("No user filter - returning all " + allProjects.size() + " projects");
        return ResponseEntity.ok(allProjects);
    }
    
    @GetMapping("/approved")
    public ResponseEntity<List<Project>> getApprovedProjects(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        if (!isManagerRole(userRole)) {
            return ResponseEntity.status(403).body(List.of());
        }
        
        List<Project> approvedProjects = projectRepository.findAll().stream()
            .filter(p -> p.getApprovalStatus() == Project.ApprovalStatus.APPROVED)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(approvedProjects);
    }
    
    @GetMapping("/vpn-pending")
    public ResponseEntity<List<Project>> getVpnPendingProjects(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        List<Project> allProjects = projectService.findAllProjects();
        
        if (isNetworkingRole(userRole) || isManagerRole(userRole)) {
            List<Project> pending = allProjects.stream()
                .filter(p -> p.getVpnStatus() == Project.VpnStatus.REQUESTED)
                .collect(Collectors.toList());
            return ResponseEntity.ok(pending);
        }
        
        if (isBusinessRole(userRole) && userId != null) {
            List<Project> pending = allProjects.stream()
                .filter(p -> p.getVpnStatus() == Project.VpnStatus.REQUESTED && 
                            p.getInitiatedBy() != null && 
                            p.getInitiatedBy().getId().equals(userId))
                .collect(Collectors.toList());
            return ResponseEntity.ok(pending);
        }
        
        return ResponseEntity.ok(List.of());
    }
    @GetMapping("/{id}")
public ResponseEntity<?> getProjectById(@PathVariable Long id) {
    return projectService.findProjectById(id)
        .map(project -> {
            Map<String, Object> response = new HashMap<>();
            
            // Basic project fields
            response.put("id", project.getId());
            response.put("projectName", project.getProjectName());
            response.put("projectType", project.getProjectType());
            response.put("description", project.getDescription());
            response.put("status", project.getStatus());
            response.put("ragStatus", project.getRagStatus());
            response.put("startDate", project.getStartDate());
            response.put("endDate", project.getEndDate());
            response.put("completionPercentage", project.getCompletionPercentage());
            response.put("createdAt", project.getCreatedAt());
            response.put("updatedAt", project.getUpdatedAt());
            response.put("vpnStatus", project.getVpnStatus() != null ? project.getVpnStatus() : "NONE");
            response.put("approvalStatus", project.getApprovalStatus() != null ? project.getApprovalStatus() : "PENDING");

            // Initiated By
            if (project.getInitiatedBy() != null) {
                Map<String, Object> initiatedByData = new HashMap<>();
                initiatedByData.put("id", project.getInitiatedBy().getId());
                initiatedByData.put("username", project.getInitiatedBy().getUsername());
                initiatedByData.put("fullName", project.getInitiatedBy().getFullName());
                initiatedByData.put("role", project.getInitiatedBy().getRole());
                response.put("initiatedBy", initiatedByData);
            } else {
                response.put("initiatedBy", null);
            }
            
            // Manager
            if (project.getManager() != null) {
                Map<String, Object> managerData = new HashMap<>();
                managerData.put("id", project.getManager().getId());
                managerData.put("username", project.getManager().getUsername());
                managerData.put("fullName", project.getManager().getFullName());
                response.put("manager", managerData);
            } else {
                response.put("manager", null);
            }
            
            // ✅ FETCH AND ADD TASKS
            try {
                List<Task> tasks = taskService.findTasksByProjectId(id);
                response.put("tasks", tasks != null ? tasks : List.of());
                System.out.println("✅ Added " + (tasks != null ? tasks.size() : 0) + " tasks to response");
            } catch (Exception e) {
                System.err.println("⚠️ Error fetching tasks: " + e.getMessage());
                response.put("tasks", List.of());
            }
            
            // ✅ FETCH AND ADD MILESTONES  
            try {
                List<Milestone> milestones = milestoneService.findMilestonesByProjectId(id);
                response.put("milestones", milestones != null ? milestones : List.of());
                System.out.println("✅ Added " + (milestones != null ? milestones.size() : 0) + " milestones to response");
            } catch (Exception e) {
                System.err.println("⚠️ Error fetching milestones: " + e.getMessage());
                response.put("milestones", List.of());
            }
            
            // ✅ FETCH AND ADD APIs
            try {
                List<Api> apis = apiService.findApisByProjectId(id);
                response.put("apis", apis != null ? apis : List.of());
                System.out.println("✅ Added " + (apis != null ? apis.size() : 0) + " APIs to response");
            } catch (Exception e) {
                System.err.println("⚠️ Error fetching APIs: " + e.getMessage());
                response.put("apis", List.of());
            }
            
            // ✅ FETCH AND ADD ATTACHMENTS (NEW!)
            try {
                List<Attachment> attachments = attachmentService.getProjectAttachments(id);
                response.put("attachments", attachments != null ? attachments : List.of());
                System.out.println("✅ Added " + (attachments != null ? attachments.size() : 0) + " attachments to response");
            } catch (Exception e) {
                System.err.println("⚠️ Error fetching attachments: " + e.getMessage());
                response.put("attachments", List.of());
            }
            
            return ResponseEntity.ok(response);
        })
        .orElse(ResponseEntity.notFound().build());
}
    
    @GetMapping("/active")
    public ResponseEntity<List<Project>> getActiveProjects() {
        return ResponseEntity.ok(projectService.findActiveProjects());
    }
    
    @GetMapping("/critical")
    public ResponseEntity<List<Project>> getCriticalProjects() {
        return ResponseEntity.ok(projectService.findCriticalProjects());
    }
    
   @PostMapping
public ResponseEntity<?> createProject(@RequestBody Map<String, Object> projectData,
                                        @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
    try {
        System.out.println("=== CREATE PROJECT REQUEST ===");
        System.out.println("🔍 Current User ID from header: " + currentUserId);
        System.out.println("Request  " + projectData);
        
        // 🔍 ✅ VALIDATE PROJECT NAME - Check for duplicates
        String projectName = (String) projectData.get("projectName");
        if (projectName == null || projectName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Project name is required"));
        }
        
        // ✅ Check if project name already exists (case-insensitive)
        if (projectRepository.existsByProjectNameIgnoreCase(projectName.trim())) {
            System.out.println("❌ Duplicate project name detected: " + projectName);
            return ResponseEntity.status(409)  // 409 = Conflict
                .body(Map.of("error", "Project name exists"));
        }
        
        Project project = new Project();
        project.setProjectName(projectName.trim());
        project.setProjectType((String) projectData.get("projectType"));
        project.setDescription((String) projectData.get("description"));
        
        if (projectData.get("startDate") != null) {
            try {
                project.setStartDate(java.time.LocalDate.parse(projectData.get("startDate").toString()));
            } catch (Exception e) {
                System.out.println("Start date parse error: " + e.getMessage());
            }
        }
        
        if (projectData.get("endDate") != null) {
            try {
                project.setEndDate(java.time.LocalDate.parse(projectData.get("endDate").toString()));
            } catch (Exception e) {
                System.out.println("End date parse error: " + e.getMessage());
            }
        }
        User initiatedBy = null;
        Object initiatedByIdObj = projectData.get("initiatedById");
        if (initiatedByIdObj != null) {
            try {
                Long initiatedById = Long.valueOf(initiatedByIdObj.toString());
                initiatedBy = userService.findUserById(initiatedById)
                    .orElseThrow(() -> new RuntimeException("Initiator user not found: " + initiatedById));
            } catch (Exception e) {
                System.out.println("Error finding initiator: " + e.getMessage());
            }
        }
        User manager = null;
        if (initiatedBy != null && isManagerRole(initiatedBy.getRole().name())) {
            manager = initiatedBy;
        }
        
        Object managerIdObj = projectData.get("managerId");
        if (managerIdObj != null) {
            try {
                Long managerId = Long.valueOf(managerIdObj.toString());
                manager = userService.findUserById(managerId)
                    .orElseThrow(() -> new RuntimeException("Manager not found: " + managerId));
            } catch (Exception e) {
                System.out.println("Error finding manager: " + e.getMessage());
            }
        }
        User currentUser = null;
        if (currentUserId != null) {
            currentUser = userService.findUserById(currentUserId).orElse(null);
            System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
        }
        if (currentUser != null) {
            project.setCreatedBy(currentUser);  
            if (initiatedBy == null) {
                initiatedBy = currentUser;
                project.setInitiatedBy(initiatedBy);
            }
            if (manager == null && currentUser.getRole() == com.nib.projecttracking.entity.User.Role.BUSINESS) {
                manager = currentUser;
                project.setManager(manager);
            }
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
        Project createdProject = projectService.createProjectWithUser(project, initiatedBy, manager, currentUser);
        
        System.out.println("✅ Project created with ID: " + createdProject.getId());
        System.out.println("✅ createdBy: " + (createdProject.getCreatedBy() != null ? createdProject.getCreatedBy().getUsername() : "NULL"));
        System.out.println("✅ manager: " + (createdProject.getManager() != null ? createdProject.getManager().getUsername() : "NULL"));
        
        return ResponseEntity.ok(Map.of(
            "message", "Project created successfully",
            "project", createdProject
        ));
        
    } catch (org.springframework.dao.DataIntegrityViolationException e) {
        // ✅ CATCH DATABASE CONSTRAINT VIOLATIONS
        System.err.println("❌ Database constraint violation: " + e.getMessage());
        if (e.getRootCause() != null && e.getRootCause().getMessage().contains("uk_project_name")) {
            return ResponseEntity.status(409)
                .body(Map.of("error", "Project name exists"));
        }
        return ResponseEntity.status(409)
            .body(Map.of("error", "Database constraint violation"));
    } catch (Exception e) {
        System.err.println("❌ Error creating project: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
    
    private boolean canEditProjectDates(String role) {
        if (role == null) return false;
        return role.equals("PROJECT_MANAGER") ||
               role.equals("CEO") ||
               role.equals("DEPUTY_CHIEF") ||
               role.equals("DIRECTOR") ||
               role.equals("CORE_BANKING_MANAGER") ||
               role.equals("DIGITAL_BANKING_MANAGER");
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveProject(
            @PathVariable Long id,
            @RequestBody Map<String, String> data,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        try {
            System.out.println("=== APPROVE PROJECT REQUEST ===");
            System.out.println("Project ID: " + id);
            System.out.println("Current User ID: " + currentUserId);
            
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
            }
            
            if (currentUser == null || currentUser.getRole() != User.Role.QUALITY_ASSURANCE) {
                return ResponseEntity.status(403).body(Map.of("error", "Only Quality Assurance can approve/reject projects"));
            }
            
            String action = data.getOrDefault("action", "APPROVE").toUpperCase();
            
            if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid action: " + action + ". Must be APPROVE or REJECT"));
            }
            
            Project updatedProject = projectService.approveOrRejectProject(id, action, currentUser);
            
            System.out.println("✅ Project " + action.toLowerCase() + " by " + currentUser.getUsername());
            
            return ResponseEntity.ok(Map.of(
                "message", "Project " + action.toLowerCase() + " successfully",
                "project", updatedProject
            ));
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ Error approving project: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/vpn-status")
    public ResponseEntity<?> updateVpnStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> data,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        try {
            System.out.println("=== UPDATE VPN STATUS REQUEST ===");
            System.out.println("Project ID: " + id);
            System.out.println("Current User ID: " + currentUserId);
            
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
            }
            
            Project project = projectService.findProjectById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
            
            String newVpnStatus = data.get("vpnStatus");
            if (newVpnStatus == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "vpnStatus is required"));
            }
            
            if (isBusinessRole(currentUser != null ? currentUser.getRole().name() : null)) {
                if (!"REQUESTED".equals(newVpnStatus)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Business users can only submit VPN requests"));
                }
                if (project.getInitiatedBy() == null || 
                    !project.getInitiatedBy().getId().equals(currentUserId)) {
                    return ResponseEntity.status(403).body(Map.of("error", "You can only request VPN for your projects"));
                }
            } else if (isNetworkingRole(currentUser != null ? currentUser.getRole().name() : null)) {
                if (!"CONFIGURED".equals(newVpnStatus) && !"REQUESTED".equals(newVpnStatus)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid VPN status transition"));
                }
            } else if (isManagerRole(currentUser != null ? currentUser.getRole().name() : null)) {
                // Managers can also update VPN status
            } else {
                return ResponseEntity.status(403).body(Map.of("error", "Unauthorized to update VPN status"));
            }
            
            Project.VpnStatus statusEnum = Project.VpnStatus.valueOf(newVpnStatus.toUpperCase());
            project.setVpnStatus(statusEnum);
            project.setUpdatedAt(LocalDateTime.now());
            
            Project updatedProject = projectService.updateProject(id, project);
            
            if (currentUser != null) {
                activityLogService.logAction(
                    currentUser,
                    "VPN_STATUS_UPDATED",
                    "Project",
                    id,
                    "VPN status changed to: " + newVpnStatus + " for project: " + project.getProjectName()
                );
            }
            
            System.out.println("✅ VPN status updated to: " + newVpnStatus + " for project ID: " + id);
            
            return ResponseEntity.ok(Map.of(
                "message", "VPN status updated successfully",
                "project", updatedProject
            ));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid VPN status value: " + data.get("vpnStatus")));
        } catch (Exception e) {
            System.err.println("❌ Error updating VPN status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(@PathVariable Long id, 
                                            @RequestBody Map<String, Object> projectData,
                                            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            System.out.println("=== UPDATE PROJECT REQUEST ===");
            System.out.println("🔍 Current User ID from header: " + currentUserId);
            System.out.println("Project ID: " + id);
            
            // ✅ VALIDATE PROJECT NAME CHANGE - Check for duplicates (excluding current project)
            String newProjectName = (String) projectData.get("projectName");
            if (newProjectName != null && !newProjectName.trim().isEmpty()) {
                // Check if another project (not this one) already has this name
                if (projectRepository.existsByProjectNameIgnoreCaseAndIdNot(newProjectName.trim(), id)) {
                    System.out.println("❌ Duplicate project name detected on update: " + newProjectName);
                    return ResponseEntity.status(409)
                        .body(Map.of("error", "Project name exists"));
                }
            }
            
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
                System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            }
           
            if (currentUser != null && !canEditProjectDates(currentUser.getRole().name())) {
                if (projectData.containsKey("startDate") || projectData.containsKey("endDate")) {
                    System.out.println("⚠️ Business user attempted to update dates - fields removed");
                    projectData.remove("startDate");
                    projectData.remove("endDate");
                }
            }
            
            Project projectDetails = new Project();
            
            if (projectData.get("projectName") != null) {
                projectDetails.setProjectName(((String) projectData.get("projectName")).trim());
            }
            if (projectData.get("projectType") != null) {
                projectDetails.setProjectType((String) projectData.get("projectType"));
            }
            if (projectData.get("description") != null) {
                projectDetails.setDescription((String) projectData.get("description"));
            }
            if (projectData.get("status") != null) {
                try {
                    projectDetails.setStatus(Project.ProjectStatus.valueOf(
                        projectData.get("status").toString().toUpperCase()
                    ));
                } catch (Exception e) {
                    System.out.println("Status parse error: " + e.getMessage());
                }
            }
            
            if (currentUser == null || canEditProjectDates(currentUser.getRole().name())) {
                if (projectData.get("startDate") != null) {
                    try {
                        projectDetails.setStartDate(java.time.LocalDate.parse(projectData.get("startDate").toString()));
                    } catch (Exception e) {
                        System.out.println("Start date parse error: " + e.getMessage());
                    }
                }
                if (projectData.get("endDate") != null) {
                    try {
                        projectDetails.setEndDate(java.time.LocalDate.parse(projectData.get("endDate").toString()));
                    } catch (Exception e) {
                        System.out.println("End date parse error: " + e.getMessage());
                    }
                }
            }
            
            if (projectData.get("completionPercentage") != null) {
                try {
                    projectDetails.setCompletionPercentage(
                        Integer.valueOf(projectData.get("completionPercentage").toString())
                    );
                } catch (Exception e) {
                    System.out.println("Completion parse error: " + e.getMessage());
                }
            }
            
            Project updatedProject = projectService.updateProjectWithUser(id, projectDetails, currentUser);
            
            System.out.println("✅ Project updated with ID: " + updatedProject.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Project updated successfully",
                "project", updatedProject
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error updating project: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/assign-manager")
    public ResponseEntity<?> assignManager(@PathVariable Long id, @RequestBody Map<String, Long> data) {
        try {
            User manager = new User();
            manager.setId(data.get("managerId"));
            Project updatedProject = projectService.assignManager(id, manager);
            return ResponseEntity.ok(Map.of(
                "message", "Manager assigned successfully",
                "project", updatedProject
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateProjectStatus(@PathVariable Long id, 
                                                  @RequestBody Map<String, String> data) {
        try {
            Project.ProjectStatus status = Project.ProjectStatus.valueOf(
                data.get("status").toUpperCase()
            );
            Project updatedProject = projectService.updateProjectStatus(id, status);
            return ResponseEntity.ok(Map.of(
                "message", "Project status updated successfully",
                "project", updatedProject
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/rag-status")
    public ResponseEntity<?> updateRagStatus(@PathVariable Long id) {
        try {
            Project updatedProject = projectService.updateRagStatus(id);
            return ResponseEntity.ok(Map.of(
                "message", "RAG status updated successfully",
                "project", updatedProject
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable Long id,
                                            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        try {
            System.out.println("=== DELETE PROJECT REQUEST ===");
            System.out.println("🔍 Current User ID from header: " + currentUserId);
      
            User currentUser = null;
            if (currentUserId != null) {
                currentUser = userService.findUserById(currentUserId).orElse(null);
                System.out.println("🔍 Loaded currentUser: " + (currentUser != null ? currentUser.getUsername() : "null"));
            }
            if (currentUser != null) {
                activityLogService.logAction(
                    currentUser,
                    "PROJECT_DELETED",
                    "Project",
                    id,
                    "Deleted project with ID: " + id
                );
            }
            projectService.deleteProjectWithUser(id, currentUser);
            
            System.out.println("✅ Project deleted with ID: " + id);
            
            return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Error deleting project: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/bulk-assign-manager")
    public ResponseEntity<?> bulkAssignManager(@RequestParam Long managerId) {
        try {
            User manager = new User();
            manager.setId(managerId);
            
            List<Project> projectsWithoutManagers = projectService.findAllProjects().stream()
                .filter(p -> p.getManager() == null)
                .collect(Collectors.toList());
            
            int updatedCount = 0;
            for (Project project : projectsWithoutManagers) {
                project.setManager(manager);
                projectService.updateProject(project.getId(), project);
                updatedCount++;
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Assigned manager to " + updatedCount + " projects",
                "managerId", managerId,
                "updatedProjects", updatedCount
            ));
        } catch (Exception e) {
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
               role.equals("PROJECT_MANAGER") ||
               role.equals("CORE_BANKING_MANAGER") ||
               role.equals("DIGITAL_BANKING_MANAGER")||
                role.equals("QUALITY_ASSURANCE");
    }
    
    private boolean isBusinessRole(String role) {
        if (role == null) return false;
        return role.equals("BUSINESS");
    }
    
    private boolean isNetworkingRole(String role) {
        if (role == null) return false;
        return role.equals("NETWORK_ADMIN") || 
               role.equals("SENIOR_IT_OFFICER") || 
               role.equals("JUNIOR_IT_OFFICER") ||
               role.equals("DIGITAL_BANKING_MANAGER");
    }
    
    @GetMapping("/debug/{id}")
    public ResponseEntity<?> debugProject(@PathVariable Long id) {
        System.out.println("🔍 DEBUG: Fetching project " + id);
        
        return projectService.findProjectById(id)
            .map(project -> {
                Map<String, Object> debug = new HashMap<>();
                debug.put("projectId", project.getId());
                debug.put("projectName", project.getProjectName());
                
                if (project.getInitiatedBy() != null) {
                    Map<String, Object> initiator = new HashMap<>();
                    initiator.put("id", project.getInitiatedBy().getId());
                    initiator.put("username", project.getInitiatedBy().getUsername());
                    initiator.put("fullName", project.getInitiatedBy().getFullName());
                    debug.put("initiatedBy", initiator);
                    System.out.println("✅ initiatedBy found: " + project.getInitiatedBy().getUsername());
                } else {
                    debug.put("initiatedBy", null);
                    System.out.println("❌ initiatedBy is NULL");
                }
                
                return ResponseEntity.ok(debug);
            })
            .orElse(ResponseEntity.notFound().build());
    }

@GetMapping("/completed")
public ResponseEntity<List<Project>> getCompletedProjects(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String userRole) {
    
    System.out.println("=== GET COMPLETED PROJECTS ===");
    System.out.println("User ID: " + userId);
    System.out.println("User Role: " + userRole);
    
    // Fetch all projects
    List<Project> allProjects = projectService.findAllProjects();
    System.out.println("📊 Total projects in database: " + allProjects.size());
    
    // ✅ Filter by COMPLETED status OR 100% completion
    List<Project> completedProjects = allProjects.stream()
        .filter(p -> {
            boolean isCompleted = p.getStatus() == Project.ProjectStatus.COMPLETED || 
                                 (p.getCompletionPercentage() != null && p.getCompletionPercentage() == 100);
            if (isCompleted) {
                System.out.println("✅ Found completed project: " + p.getProjectName() + 
                                 " | Status: " + p.getStatus() + 
                                 " | Completion: " + p.getCompletionPercentage() + "%");
            }
            return isCompleted;
        })
        .collect(Collectors.toList());
    
    System.out.println("📦 Completed projects count: " + completedProjects.size());
    
    // Apply user role filtering
    if (userId != null && userRole != null) {
        if (isBusinessRole(userRole)) {
            completedProjects = completedProjects.stream()
                .filter(p -> p.getInitiatedBy() != null && 
                            p.getInitiatedBy().getId().equals(userId))
                .collect(Collectors.toList());
            System.out.println("📊 After business filter: " + completedProjects.size());
        } else if (isTechnicalRole(userRole)) {
            User user = new User();
            user.setId(userId);
            List<Task> userTasks = taskService.findTasksByAssignedUser(user);
            
            completedProjects = userTasks.stream()
                .map(Task::getProject)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() == Project.ProjectStatus.COMPLETED || 
                            (p.getCompletionPercentage() != null && p.getCompletionPercentage() == 100))
                .distinct()
                .collect(Collectors.toList());
            System.out.println("📊 After technical filter: " + completedProjects.size());
        }
    }
    
    return ResponseEntity.ok(completedProjects);
}
}