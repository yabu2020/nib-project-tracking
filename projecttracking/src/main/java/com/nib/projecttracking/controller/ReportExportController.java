package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.ProgressUpdate;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.service.MilestoneService;
import com.nib.projecttracking.service.ProjectService;
import com.nib.projecttracking.service.ReportExportService;
import com.nib.projecttracking.service.TaskService;
import com.nib.projecttracking.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nib.projecttracking.service.ProgressUpdateService;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports/export")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ReportExportController {
    
    @Autowired
    private ReportExportService exportService;
    
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private MilestoneService milestoneService;
     @Autowired  
    private ProgressUpdateService progressUpdateService;
 
    
    @GetMapping("/projects/pdf")
    public ResponseEntity<byte[]> exportProjectsPdf(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        List<Project> projects = projectService.findAllProjects();
        byte[] pdfContent = exportService.exportProjectsToPdf(projects);
        
        String filename = "projects-report-" + LocalDate.now() + ".pdf";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }
    
    @GetMapping("/projects/excel")
    public ResponseEntity<byte[]> exportProjectsExcel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        List<Project> projects = projectService.findAllProjects();
        byte[] excelContent = exportService.exportProjectsToExcel(projects);
        
        String filename = "projects-report-" + LocalDate.now() + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok().headers(headers).body(excelContent);
    }
    
  
    
    @GetMapping("/tasks/excel")
    public ResponseEntity<byte[]> exportTasksExcel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        List<Task> tasks = taskService.findAllTasks();
        byte[] excelContent = exportService.exportTasksToExcel(tasks);
        
        String filename = "tasks-report-" + LocalDate.now() + ".xlsx";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return ResponseEntity.ok().headers(headers).body(excelContent);
    }

    
   @GetMapping("/dashboard/pdf")
public ResponseEntity<byte[]> exportDashboardPdf(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String userRole,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate, 
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {  
        
        try {
            List<Project> projects = projectService.findAllProjects();
            List<Task> tasks = taskService.findAllTasks();
            List<User> users = userService.findAllUsers();
            
            Map<String, Object> summary = buildDashboardSummary(projects, tasks);
            List<Map<String, Object>> teamPerformance = exportService.calculateTeamPerformance(tasks, users);
            
            byte[] pdfContent = exportService.exportDashboardSummaryToPdf(summary, teamPerformance, null, null);
            
            String filename = "dashboard-summary-" + LocalDate.now() + ".pdf";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok().headers(headers).body(pdfContent);
            
        } catch (Exception e) {
            System.err.println("❌ PDF Export Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/dashboard/excel")
    public ResponseEntity<byte[]> exportDashboardExcel(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        try {
            List<Project> projects = projectService.findAllProjects();
            List<Task> tasks = taskService.findAllTasks();
            List<User> users = userService.findAllUsers();
            
            Map<String, Object> summary = buildDashboardSummary(projects, tasks);
            List<Map<String, Object>> teamPerformance = exportService.calculateTeamPerformance(tasks, users);
            
            byte[] excelContent = exportService.exportDashboardSummaryToExcel(summary, teamPerformance, null, null);
            
            String filename = "dashboard-summary-" + LocalDate.now() + ".xlsx";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            
            return ResponseEntity.ok().headers(headers).body(excelContent);
            
        } catch (Exception e) {
            System.err.println("❌ Excel Export Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    
    @GetMapping("/team-performance")
    public ResponseEntity<List<Map<String, Object>>> getTeamPerformance(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole) {
        
        try {
            List<Task> tasks = taskService.findAllTasks();
            List<User> users = userService.findAllUsers();
            
            List<Map<String, Object>> teamPerformance = exportService.calculateTeamPerformance(tasks, users);
            
            return ResponseEntity.ok(teamPerformance);
            
        } catch (Exception e) {
            System.err.println("❌ Team Performance Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    
    /**
 * Build dashboard summary map from projects and tasks
 */
/**
 * Build dashboard summary map from projects and tasks
 */
private Map<String, Object> buildDashboardSummary(List<Project> projects, List<Task> tasks) {
    Map<String, Object> summary = new HashMap<>();
    
    System.out.println("=== Building Dashboard Summary ===");
    System.out.println("Total Projects: " + projects.size());
    System.out.println("Total Tasks: " + tasks.size());
    
  
    long totalProjects = projects.size();
    long activeProjects = projects.stream()
        .filter(p -> p.getStatus() != null && 
            ("IN_PROGRESS".equals(p.getStatus().toString()) || "PLANNED".equals(p.getStatus().toString())))
        .count();
    long criticalProjects = projects.stream()
        .filter(p -> p.getRagStatus() != null && 
            ("RED".equals(p.getRagStatus().toString()) || "AMBER".equals(p.getRagStatus().toString())))
        .count();
    long completedProjects = projects.stream()
        .filter(p -> p.getStatus() != null && "COMPLETED".equals(p.getStatus().toString()))
        .count();
    

    long overdueTasks = tasks.stream()
        .filter(t -> t.getDueDate() != null && 
            t.getDueDate().isBefore(LocalDate.now()) &&
            t.getStatus() != null &&
            !"COMPLETED".equals(t.getStatus().toString()))
        .count();
    
   
long activeBlockers = 0;
try {
    
    List<ProgressUpdate> allUpdates = progressUpdateService.findAll();
    System.out.println("Total Progress Updates: " + allUpdates.size());
    
   
    activeBlockers = allUpdates.stream()
        .filter(pu -> pu.getBlockers() != null && 
                     !pu.getBlockers().trim().isEmpty() &&
                     !"null".equals(pu.getBlockers()) &&
                     pu.getBlockers().length() > 0)
        .count();
    
    System.out.println("Active Blockers from ProgressUpdates: " + activeBlockers);
    
    
    allUpdates.stream()
        .filter(pu -> pu.getBlockers() != null && !pu.getBlockers().trim().isEmpty())
        .forEach(pu -> System.out.println("  Blocker: " + pu.getBlockers()));
    
} catch (Exception e) {
    System.err.println("Error calculating active blockers: " + e.getMessage());
    e.printStackTrace();
    activeBlockers = 0;
}
    
    long overdueMilestones = 0;
    try {
        if (milestoneService != null) {
            List<Milestone> allMilestones = milestoneService.findAllMilestones();
            System.out.println("Total Milestones: " + allMilestones.size());
       
            for (Milestone m : allMilestones) {
                System.out.println("  Milestone: " + m.getMilestoneName() + 
                                 " | Target: " + m.getTargetDate() + 
                                 " | Status: " + m.getStatus() +
                                 " | Is Today or Before: " + (m.getTargetDate() != null && !m.getTargetDate().isAfter(LocalDate.now())));
            }
            
            overdueMilestones = allMilestones.stream()
                .filter(m -> {
                   
                    boolean isDateOverdue = m.getTargetDate() != null && 
                                          !m.getTargetDate().isAfter(LocalDate.now());
                 
                    boolean isNotCompleted = m.getStatus() != null &&
                                           !"COMPLETED".equals(m.getStatus().toString()) &&
                                           !"CANCELLED".equals(m.getStatus().toString());
                    
                    return isDateOverdue && isNotCompleted;
                })
                .count();
            
            System.out.println("Overdue Milestones calculated: " + overdueMilestones);
        } else {
            System.out.println("MilestoneService is null");
        }
    } catch (Exception e) {
        System.err.println("Error calculating overdue milestones: " + e.getMessage());
        e.printStackTrace();
        overdueMilestones = 0;
    }
    
    
    long apisInDev = projects.stream()
        .filter(p -> "API".equals(p.getProjectType()))
        .count();
    
   
    Map<String, Long> ragStatus = new HashMap<>();
    ragStatus.put("GREEN", projects.stream()
        .filter(p -> p.getRagStatus() != null && "GREEN".equals(p.getRagStatus().toString())).count());
    ragStatus.put("AMBER", projects.stream()
        .filter(p -> p.getRagStatus() != null && "AMBER".equals(p.getRagStatus().toString())).count());
    ragStatus.put("RED", projects.stream()
        .filter(p -> p.getRagStatus() != null && "RED".equals(p.getRagStatus().toString())).count());
    
    summary.put("totalProjects", totalProjects);
    summary.put("activeProjects", activeProjects);
    summary.put("criticalProjects", criticalProjects);
    summary.put("completedProjects", completedProjects);
    summary.put("overdueTasks", overdueTasks);
    summary.put("overdueMilestones", overdueMilestones);
    summary.put("activeBlockers", activeBlockers);
    summary.put("apisInDevelopment", apisInDev);
    summary.put("ragStatus", ragStatus);
    
    System.out.println("=== Dashboard Summary Built ===");
    System.out.println("Overdue Milestones: " + overdueMilestones);
    System.out.println("Active Blockers: " + activeBlockers);
    
    return summary;
}
}