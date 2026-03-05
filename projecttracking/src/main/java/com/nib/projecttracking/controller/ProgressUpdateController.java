
package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.ProgressUpdate;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.ProgressUpdateService;
import com.nib.projecttracking.service.ProjectService;
import com.nib.projecttracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/progress-updates")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ProgressUpdateController {
    
    @Autowired
    private ProgressUpdateService progressUpdateService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserService userService;
 
    
    /**
     * ✅ Get all progress updates with role-based filtering
     */
    @GetMapping
    public ResponseEntity<List<ProgressUpdate>> getAllUpdates(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long submittedById,  
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        try {
            System.out.println("=== GET PROGRESS UPDATES ===");
            System.out.println("userId: " + userId + ", userRole: " + userRole);
            System.out.println("projectId: " + projectId + ", submittedById: " + submittedById);
            
            List<ProgressUpdate> updates;
            
         
            if (isTechnicalRole(userRole) && userId != null) {
                User user = new User();
                user.setId(userId);
                updates = progressUpdateService.findByUser(user);
                System.out.println("🔧 Technical staff - returning " + updates.size() + " own updates");
            }
            
            else if (isManagerRole(userRole)) {
                if (submittedById != null) {
                    
                    User teamMember = new User();
                    teamMember.setId(submittedById);
                    updates = progressUpdateService.findByUser(teamMember);
                    System.out.println("👔 Manager filtering by team member ID: " + submittedById);
                } else if (projectId != null) {
                   
                    Project project = new Project();
                    project.setId(projectId);
                    updates = progressUpdateService.findByProject(project);
                    System.out.println("👔 Manager filtering by project ID: " + projectId);
                } else if (userId != null) {
                    
                    User user = new User();
                    user.setId(userId);
                    updates = progressUpdateService.findByUser(user);
                    System.out.println("👔 Manager viewing own updates");
                } else {
                    
                    updates = progressUpdateService.findAll();
                    System.out.println("👔 Manager viewing all updates: " + updates.size());
                }
            }
            
            else if (userId != null) {
                User user = new User();
                user.setId(userId);
                updates = progressUpdateService.findByUser(user);
                System.out.println("👤 User viewing own updates: " + updates.size());
            }
            
            else {
                updates = new ArrayList<>();
                System.out.println("⚠️ No user context - returning empty list");
            }
            
            List<ProgressUpdate> enrichedUpdates = updates.stream()
                .map(this::enrichUpdateWithSubmitterInfo)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enrichedUpdates);
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching progress updates: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * ✅ Helper: Enrich update with submitter name/role for frontend
     */
    private ProgressUpdate enrichUpdateWithSubmitterInfo(ProgressUpdate update) {
        if (update == null || update.getSubmittedBy() == null) {
            return update;
        }
        
      
        try {
            User submitter = update.getSubmittedBy();
            
            if (submitter.getId() != null) {
                
                submitter.getUsername(); 
            }
        } catch (Exception e) {
            System.out.println("⚠️ Could not enrich submitter info: " + e.getMessage());
        }
        
        return update;
    }
    
    /**
     * ✅ Get update by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUpdateById(@PathVariable Long id) {
        try {
            Optional<ProgressUpdate> update = progressUpdateService.findById(id);
            return update.map(this::enrichUpdateWithSubmitterInfo)
                        .map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * ✅ Get updates by project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProgressUpdate>> getUpdatesByProject(@PathVariable Long projectId) {
        try {
            Project project = new Project();
            project.setId(projectId);
            List<ProgressUpdate> updates = progressUpdateService.findByProject(project);
            
         
            List<ProgressUpdate> enriched = updates.stream()
                .map(this::enrichUpdateWithSubmitterInfo)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * ✅ Get updates by user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ProgressUpdate>> getUpdatesByUser(@PathVariable Long userId) {
        try {
            User user = new User();
            user.setId(userId);
            List<ProgressUpdate> updates = progressUpdateService.findByUser(user);
            
            
            List<ProgressUpdate> enriched = updates.stream()
                .map(this::enrichUpdateWithSubmitterInfo)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * ✅ Get updates with blockers
     */
    @GetMapping("/blockers")
    public ResponseEntity<List<ProgressUpdate>> getUpdatesWithBlockers() {
        try {
            List<ProgressUpdate> updates = progressUpdateService.findUpdatesWithBlockers();
            
     
            List<ProgressUpdate> enriched = updates.stream()
                .map(this::enrichUpdateWithSubmitterInfo)
                .collect(Collectors.toList());
            
            System.out.println("Found " + enriched.size() + " updates with blockers");
            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    /**
     * ✅ Get updates by date range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ProgressUpdate>> getUpdatesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<ProgressUpdate> updates = progressUpdateService.findByDateRange(startDate, endDate);
            
          
            List<ProgressUpdate> enriched = updates.stream()
                .map(this::enrichUpdateWithSubmitterInfo)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    
    
    /**
     * ✅ Create progress update - Technical staff submit to management
     */
    @PostMapping
    public ResponseEntity<?> createProgressUpdate(@RequestBody Map<String, Object> updateData) {
        try {
            System.out.println("=== CREATE PROGRESS UPDATE ===");
            System.out.println("Request data: " + updateData);
            
            ProgressUpdate update = new ProgressUpdate();
            
            if (updateData.get("completedWork") != null) {
                update.setCompletedWork((String) updateData.get("completedWork"));
            }
            if (updateData.get("ongoingWork") != null) {
                update.setOngoingWork((String) updateData.get("ongoingWork"));
            }
            if (updateData.get("blockers") != null) {
                update.setBlockers((String) updateData.get("blockers"));
            }
            if (updateData.get("estimatedResolution") != null) {
                try {
                    update.setEstimatedResolution(
                        LocalDate.parse(updateData.get("estimatedResolution").toString())
                    );
                } catch (Exception e) {
                    System.out.println("Date parse error for estimatedResolution");
                }
            }
            
            
            if (updateData.get("updateDate") != null) {
                try {
                    update.setUpdateDate(
                        LocalDate.parse(updateData.get("updateDate").toString())
                    );
                } catch (Exception e) {
                    System.out.println("Date parse error, using today: " + e.getMessage());
                    update.setUpdateDate(LocalDate.now());
                }
            } else {
                update.setUpdateDate(LocalDate.now());
            }
            
         
            Project project = null;
            if (updateData.get("projectId") != null) {
                Long projectId = Long.valueOf(updateData.get("projectId").toString());
                project = projectService.findProjectById(projectId)
                    .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "projectId is required"));
            }
            
           
            User submitter = null;
            if (updateData.get("submittedById") != null) {
                Long uid = Long.valueOf(updateData.get("submittedById").toString());
                submitter = userService.findUserById(uid)
                    .orElseThrow(() -> new RuntimeException("User not found: " + uid));
            } else if (updateData.get("userId") != null) {
                
                Long uid = Long.valueOf(updateData.get("userId").toString());
                submitter = userService.findUserById(uid)
                    .orElseThrow(() -> new RuntimeException("User not found: " + uid));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "submittedById or userId is required"));
            }
            
            String submitterRole = submitter.getRole() != null ? submitter.getRole().name() : "";
            if (isTechnicalRole(submitterRole)) {
                update.setSubmittedToManagement(true);
                System.out.println("✅ Technical staff update marked for management review");
            }
            
            
            ProgressUpdate createdUpdate = progressUpdateService.createProgressUpdate(update, project, submitter);
           
            ProgressUpdate enrichedUpdate = enrichUpdateWithSubmitterInfo(createdUpdate);
            
            System.out.println("✅ Progress update created with ID: " + enrichedUpdate.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Progress update submitted to management",
                "update", enrichedUpdate
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error creating progress update: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * ✅ Update progress update
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProgressUpdate(@PathVariable Long id, 
                                                   @RequestBody Map<String, Object> updateData) {
        try {
            ProgressUpdate existingUpdate = progressUpdateService.findById(id)
                .orElseThrow(() -> new RuntimeException("Progress update not found: " + id));
            
          
            if (updateData.get("completedWork") != null) {
                existingUpdate.setCompletedWork((String) updateData.get("completedWork"));
            }
            if (updateData.get("ongoingWork") != null) {
                existingUpdate.setOngoingWork((String) updateData.get("ongoingWork"));
            }
            if (updateData.get("blockers") != null) {
                existingUpdate.setBlockers((String) updateData.get("blockers"));
            }
            if (updateData.get("estimatedResolution") != null) {
                try {
                    existingUpdate.setEstimatedResolution(
                        LocalDate.parse(updateData.get("estimatedResolution").toString())
                    );
                } catch (Exception e) {
                    System.out.println("Date parse error for estimatedResolution");
                }
            }
            
            ProgressUpdate updatedUpdate = progressUpdateService.updateProgressUpdate(id, existingUpdate);
          
            ProgressUpdate enrichedUpdate = enrichUpdateWithSubmitterInfo(updatedUpdate);
            
            return ResponseEntity.ok(Map.of(
                "message", "Progress update updated successfully",
                "update", enrichedUpdate
            ));
            
        } catch (Exception e) {
            System.err.println("❌ Error updating progress update: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

   @PutMapping("/{id}/blocker-status")
public ResponseEntity<?> updateBlockerStatus(
    @PathVariable Long id,
    @RequestBody Map<String, Object> request,
    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    
    try {
        ProgressUpdate updatedUpdate = progressUpdateService.updateBlockerStatus(id, request, userId);
        return ResponseEntity.ok().body(Map.of(
            "success", true,
            "message", "Blocker status updated successfully",
            "update", updatedUpdate
        ));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "success", false,
            "error", "Failed to update blocker status: " + e.getMessage()
        ));
    }
}
@PostMapping("/{id}/comments")
public ResponseEntity<?> addComment(
    @PathVariable Long id,
    @RequestBody Map<String, Object> commentData,
    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    
    try {
        String comment = (String) commentData.get("comment");
        String commenterRole = (String) commentData.get("commenterRole");
        
        progressUpdateService.addComment(id, comment, userId, commenterRole);
        return ResponseEntity.ok(Map.of("success", true, "message", "Comment added"));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
@GetMapping("/{id}/comments")
public ResponseEntity<?> getComments(@PathVariable Long id) {
    try {
        List<Map<String, Object>> comments = progressUpdateService.getComments(id);
        return ResponseEntity.ok(comments);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

@PutMapping("/{id}/acknowledge")
public ResponseEntity<?> acknowledgeUpdate(
    @PathVariable Long id,
    @RequestHeader(value = "X-User-Id", required = false) Long userId) {
    
    try {
        progressUpdateService.acknowledgeUpdate(id, userId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Update acknowledged"));
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
    /**
     * ✅ Delete progress update
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProgressUpdate(@PathVariable Long id) {
        try {
            progressUpdateService.deleteProgressUpdate(id);
            return ResponseEntity.ok(Map.of("message", "Progress update deleted successfully"));
        } catch (Exception e) {
            System.err.println("❌ Error deleting progress update: " + e.getMessage());
            e.printStackTrace();
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
               role.equals("DIGITAL_BANKING_MANAGER");
    }
}