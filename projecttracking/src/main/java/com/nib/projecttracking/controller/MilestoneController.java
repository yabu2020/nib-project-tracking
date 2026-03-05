package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.service.MilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/milestones")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class MilestoneController {
    
    @Autowired
    private MilestoneService milestoneService;
    
    
 
    @GetMapping
    public ResponseEntity<List<Milestone>> getAllMilestones() {
        return ResponseEntity.ok(milestoneService.findAllMilestones());
    }
    
  
    @GetMapping("/{id}")
    public ResponseEntity<?> getMilestoneById(@PathVariable Long id) {
        return milestoneService.findMilestoneById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
   
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<Milestone>> getMilestonesByProject(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return ResponseEntity.ok(milestoneService.findMilestonesByProject(project));
    }
    
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Milestone>> getMilestonesByStatus(@PathVariable Milestone.MilestoneStatus status) {
        return ResponseEntity.ok(milestoneService.findMilestonesByStatus(status));
    }
    
  
    @GetMapping("/project/{projectId}/completed")
    public ResponseEntity<List<Milestone>> getCompletedMilestones(@PathVariable Long projectId) {
        Project project = new Project();
        project.setId(projectId);
        return ResponseEntity.ok(milestoneService.findCompletedMilestones(project));
    }
    
    
    @GetMapping("/overdue")
    public ResponseEntity<List<Milestone>> getOverdueMilestones() {
        return ResponseEntity.ok(milestoneService.findOverdueMilestones());
    }
    

    @GetMapping("/upcoming")
    public ResponseEntity<List<Milestone>> getUpcomingMilestones() {
        return ResponseEntity.ok(milestoneService.findUpcomingMilestones());
    }
    
   
    @GetMapping("/date-range")
    public ResponseEntity<List<Milestone>> getMilestonesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(milestoneService.findMilestonesByDateRange(startDate, endDate));
    }
    
  
    @PostMapping
    public ResponseEntity<?> createMilestone(@RequestBody Map<String, Object> milestoneData) {
        try {
            System.out.println("=== CREATE MILESTONE REQUEST ===");
            System.out.println("Request data: " + milestoneData);
            
            Milestone milestone = new Milestone();
            milestone.setMilestoneName((String) milestoneData.get("milestoneName"));
            milestone.setDescription((String) milestoneData.get("description"));
            
            if (milestoneData.get("targetDate") != null) {
                try {
                    milestone.setTargetDate(
                        LocalDate.parse(milestoneData.get("targetDate").toString())
                    );
                } catch (Exception e) {
                    System.out.println("Target date parse error: " + e.getMessage());
                }
            }
            
            if (milestoneData.get("completionPercentage") != null) {
                milestone.setCompletionPercentage(
                    Integer.valueOf(milestoneData.get("completionPercentage").toString())
                );
            }
            
            Project project = new Project();
            if (milestoneData.get("projectId") != null) {
                project.setId(Long.valueOf(milestoneData.get("projectId").toString()));
            }
            
            Milestone createdMilestone = milestoneService.createMilestone(milestone, project);
            
            System.out.println("Milestone created successfully with ID: " + createdMilestone.getId());
            
            return ResponseEntity.ok(Map.of(
                "message", "Milestone created successfully",
                "milestone", createdMilestone
            ));
        } catch (Exception e) {
            System.err.println("Error creating milestone: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateMilestoneStatus(@PathVariable Long id, 
                                                    @RequestBody Map<String, String> data) {
        try {
            Milestone.MilestoneStatus status = Milestone.MilestoneStatus.valueOf(data.get("status"));
            Milestone updatedMilestone = milestoneService.updateMilestoneStatus(id, status);
            return ResponseEntity.ok(Map.of(
                "message", "Milestone status updated successfully",
                "milestone", updatedMilestone
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
   
    @PutMapping("/{id}/progress")
    public ResponseEntity<?> updateMilestoneProgress(@PathVariable Long id, 
                                                      @RequestBody Map<String, Integer> data) {
        try {
            System.out.println("=== UPDATE MILESTONE PROGRESS ===");
            System.out.println("Milestone ID: " + id);
            System.out.println("New progress: " + data.get("completionPercentage"));
            
            int completionPercentage = data.get("completionPercentage");
            Milestone updatedMilestone = milestoneService.updateMilestoneProgress(id, completionPercentage);
            
            System.out.println("✅ Milestone updated successfully");
            
            return ResponseEntity.ok(Map.of(
                "message", "Milestone progress updated successfully",
                "milestone", updatedMilestone
            ));
        } catch (Exception e) {
            System.err.println("❌ Error updating milestone progress: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
 
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMilestone(@PathVariable Long id, 
                                              @RequestBody Map<String, Object> milestoneData) {
        try {
            Milestone milestoneDetails = new Milestone();
            milestoneDetails.setMilestoneName((String) milestoneData.get("milestoneName"));
            milestoneDetails.setDescription((String) milestoneData.get("description"));
            
            if (milestoneData.get("targetDate") != null) {
                milestoneDetails.setTargetDate(
                    LocalDate.parse(milestoneData.get("targetDate").toString())
                );
            }
            
            if (milestoneData.get("completionPercentage") != null) {
                milestoneDetails.setCompletionPercentage(
                    Integer.valueOf(milestoneData.get("completionPercentage").toString())
                );
            }
            
            Milestone updatedMilestone = milestoneService.updateMilestone(id, milestoneDetails);
            return ResponseEntity.ok(Map.of(
                "message", "Milestone updated successfully",
                "milestone", updatedMilestone
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
   
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMilestone(@PathVariable Long id) {
        try {
            milestoneService.deleteMilestone(id);
            return ResponseEntity.ok(Map.of("message", "Milestone deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}