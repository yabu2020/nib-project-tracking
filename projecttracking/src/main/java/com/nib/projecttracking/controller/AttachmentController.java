

package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Attachment;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.ActivityLogService;
import com.nib.projecttracking.service.AttachmentService;
import com.nib.projecttracking.service.ProjectService;
import com.nib.projecttracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class AttachmentController {
    
    @Autowired
    private AttachmentService attachmentService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ActivityLogService activityLogService;
    
   
    private User getCurrentUser(Long userIdFromHeader) {
        if (userIdFromHeader != null) {
            return userService.findUserById(userIdFromHeader).orElse(null);
        }
        
       
        try {
            Object principal = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
        } catch (Exception e) {
            
        }
        return null;
    }
    

    private boolean isTechnicalStaff(User user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole() == User.Role.DEVELOPER ||
               user.getRole() == User.Role.SENIOR_IT_OFFICER ||
               user.getRole() == User.Role.JUNIOR_IT_OFFICER ||
               user.getRole() == User.Role.IT_GRADUATE_TRAINEE;
    }
    
    /**
     * Check if user is business role (restricted permissions)
     */
    private boolean isBusinessRole(User user) {
        if (user == null || user.getRole() == null) return false;
        return user.getRole() == User.Role.BUSINESS ||
               user.getRole() == User.Role.QUALITY_ASSURANCE;
    }
    
    /**
     * Get client IP for logging
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
           
        }
        return "unknown";
    }
    
  
@PostMapping("/project/{projectId}")
public ResponseEntity<?> uploadToProject(
        @PathVariable Long projectId,
        @RequestParam Long userId,
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String description,
        @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
    
    try {
        System.out.println("=== ATTACHMENT UPLOAD REQUEST ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("Current User ID: " + currentUserId);
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 10MB limit"));
        }
        
      
        User currentUser = getCurrentUser(currentUserId);
        if (currentUser == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
        }
        
    
        Project project = projectService.findProjectById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        if (currentUser.getRole() == User.Role.BUSINESS || 
            currentUser.getRole() == User.Role.QUALITY_ASSURANCE) {
            
            if (project.getInitiatedBy() == null || 
                !project.getInitiatedBy().getId().equals(currentUser.getId())) {
                
                System.out.println("⚠️ Business user attempted to upload to project they didn't create");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only upload files to projects you created"));
            }
            System.out.println("✅ Business user uploading to their own project");
        }
        
       
        Attachment attachment = attachmentService.uploadToProject(projectId, userId, file, description);
        
      
        
        System.out.println("✅ File uploaded: " + attachment.getOriginalFileName());
        
        return ResponseEntity.ok(Map.of(
            "message", "File uploaded successfully",
            "attachment", attachment
        ));
        
    } catch (Exception e) {
        System.err.println("❌ Upload error: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
    /**
     * Upload file to task - WITH ROLE-BASED PERMISSIONS
     */
    @PostMapping("/task/{taskId}")
    public ResponseEntity<?> uploadToTask(
            @PathVariable Long taskId,
            @RequestParam Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size exceeds 10MB limit"));
            }
            
           
            User currentUser = getCurrentUser(currentUserId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
            }
            
            
            if (isBusinessRole(currentUser) && !isTechnicalStaff(currentUser)) {
                
            }
            
            Attachment attachment = attachmentService.uploadToTask(taskId, userId, file, description);
            
         
            activityLogService.logAction(
                currentUser,
                "FILE_UPLOADED",
                "Attachment",
                attachment.getId(),
                "Uploaded file: " + attachment.getOriginalFileName() + " to task #" + taskId,
                getClientIpAddress()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "attachment", attachment
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
   
    
    /**
     * Download attachment - Anyone with project access can download
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        try {
            Path filePath = attachmentService.getAttachmentFile(id);
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            Attachment attachment = attachmentService.getAttachment(id).orElseThrow();
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + attachment.getOriginalFileName() + "\"")
                .body(resource);
                
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    
    
    /**
     * Get all attachments for a project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Attachment>> getProjectAttachments(@PathVariable Long projectId) {
        return ResponseEntity.ok(attachmentService.getProjectAttachments(projectId));
    }
    
    /**
     * Get all attachments for a task
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<Attachment>> getTaskAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(attachmentService.getTaskAttachments(taskId));
    }
    
    /**
     * Get attachment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAttachment(@PathVariable Long id) {
        return attachmentService.getAttachment(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    
    /**
     * Delete attachment - WITH PERMISSION CHECKS
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAttachment(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId) {
        
        try {
            System.out.println("=== ATTACHMENT DELETE REQUEST ===");
            System.out.println("Attachment ID: " + id);
            System.out.println("Current User ID: " + currentUserId);
            
           
            User currentUser = getCurrentUser(currentUserId);
            if (currentUser == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not authenticated"));
            }
            
            Attachment attachment = attachmentService.getAttachment(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + id));
            
            boolean canDelete = false;
            String reason = "";
            
           
            if (isTechnicalStaff(currentUser)) {
                canDelete = true;
                reason = "Technical staff permission";
            }
            
            else if (isBusinessRole(currentUser)) {
                if (attachment.getUploadedBy() != null && 
                    attachment.getUploadedBy().getId().equals(currentUser.getId())) {
                    canDelete = true;
                    reason = "Owner of attachment";
                } else {
                    reason = "Not owner of attachment";
                }
            }
            
            else if (currentUser.getRole() == User.Role.PROJECT_MANAGER ||
                     currentUser.getRole() == User.Role.CEO ||
                     currentUser.getRole() == User.Role.DIRECTOR) {
                if (attachment.getProject() != null && 
                    attachment.getProject().getManager() != null &&
                    attachment.getProject().getManager().getId().equals(currentUser.getId())) {
                    canDelete = true;
                    reason = "Project manager permission";
                }
            }
            
            if (!canDelete) {
                System.out.println("⚠️ User " + currentUser.getUsername() + 
                    " denied delete permission: " + reason);
                
                
                activityLogService.logAction(
                    currentUser,
                    "DELETE_DENIED",
                    "Attachment",
                    id,
                    "User attempted to delete attachment #" + id + " (" + reason + ")",
                    getClientIpAddress()
                );
                
               
            }
            
            System.out.println("✅ Delete allowed: " + reason);
          
            String fileName = attachment.getOriginalFileName();
            Long projectId = attachment.getProject() != null ? attachment.getProject().getId() : null;
            
            
            attachmentService.deleteAttachment(id);
            
            activityLogService.logAction(
                currentUser,
                "FILE_DELETED",
                "Attachment",
                id,
                "Deleted file: " + fileName + 
                    (projectId != null ? " from project #" + projectId : ""),
                getClientIpAddress()
            );
            
            System.out.println("✅ Attachment deleted: " + fileName);
            
            return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully"));
            
        } catch (Exception e) {
            System.err.println("❌ Delete error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
}