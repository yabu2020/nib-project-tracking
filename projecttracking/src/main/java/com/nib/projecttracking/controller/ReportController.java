package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ReportController {
    @Autowired
    private ProjectService projectService;
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private ProgressUpdateService progressUpdateService;
    
    @Autowired
    private MilestoneService milestoneService;
    
    @Autowired
    private ApiService apiService;
    
    @Autowired
    private UserService userService;
    
    
    @GetMapping("/dashboard-summary")
    public ResponseEntity<Map<String, Object>> getDashboardSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Map<String, Object> summary = new HashMap<>();

        try {
            List<Project> allProjects = projectService.findAllProjects();
            List<Project> userProjects = allProjects;

            if (userId != null && userRole != null && isTechnicalRole(userRole)) {

                List<Task> allTasks = taskService.findAllTasks();
                Set<Long> projectIds = new HashSet<>();

                for (Task task : allTasks) {
                    if (task.getAssignedTo() != null &&
                        task.getAssignedTo().getId() != null &&
                        task.getAssignedTo().getId().equals(userId) &&
                        task.getProject() != null) {

                        projectIds.add(task.getProject().getId());
                    }
                }

                userProjects = allProjects.stream()
                        .filter(p -> projectIds.contains(p.getId()))
                        .collect(Collectors.toList());
            }
            else if (isBusinessRole(userRole)) {
    userProjects = allProjects.stream()
            .filter(p -> {
                return (p.getManager() != null && 
                        p.getManager().getId() != null && 
                        p.getManager().getId().equals(userId)) ||
                       (p.getCreatedBy() != null && 
                        p.getCreatedBy().getId() != null && 
                        p.getCreatedBy().getId().equals(userId));
            })
            .collect(Collectors.toList());
}

            summary.put("totalProjects", userProjects.size());

            long activeCount = userProjects.stream()
                    .filter(p -> p.getStatus() != null &&
                            (p.getStatus() == Project.ProjectStatus.PLANNED ||
                             p.getStatus() == Project.ProjectStatus.IN_PROGRESS))
                    .count();
            summary.put("activeProjects", activeCount);

            long criticalCount = userProjects.stream()
                    .filter(p -> p.getRagStatus() != null &&
                            (p.getRagStatus() == Project.RagStatus.RED ||
                             p.getRagStatus() == Project.RagStatus.AMBER))
                    .count();
            summary.put("criticalProjects", criticalCount);

            long completedCount = userProjects.stream()
                    .filter(p -> p.getStatus() == Project.ProjectStatus.COMPLETED)
                    .count();
            summary.put("completedProjects", completedCount);

            Map<String, Long> ragStatus = new HashMap<>();
            ragStatus.put("GREEN", userProjects.stream().filter(p -> p.getRagStatus() == Project.RagStatus.GREEN).count());
            ragStatus.put("AMBER", userProjects.stream().filter(p -> p.getRagStatus() == Project.RagStatus.AMBER).count());
            ragStatus.put("RED", userProjects.stream().filter(p -> p.getRagStatus() == Project.RagStatus.RED).count());
            summary.put("ragStatus", ragStatus);

             long overdueTasks = 0;
        if (userId != null) {
            if (isTechnicalRole(userRole) || isBusinessRole(userRole)) {
                overdueTasks = taskService.findAllTasks().stream()
                        .filter(t -> t.getAssignedTo() != null &&
                                t.getAssignedTo().getId() != null &&
                                t.getAssignedTo().getId().equals(userId) &&
                                t.getDueDate() != null &&
                                t.getDueDate().isBefore(LocalDate.now()) &&
                                t.getStatus() != Task.TaskStatus.COMPLETED)
                        .count();
            } else {
                overdueTasks = taskService.countOverdueTasks();
            }
        }
            summary.put("overdueTasks", overdueTasks);

            summary.put("overdueMilestones", milestoneService.countOverdueMilestones());
            summary.put("upcomingMilestones", milestoneService.countUpcomingMilestones());
            summary.put("activeBlockers", progressUpdateService.countActiveBlockers());
            summary.put("apisInDevelopment", apiService.countActiveDevelopmentApis());

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/rag-status")
    public ResponseEntity<Map<String, Object>> getRagStatusReport(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            List<Project> allProjects = projectService.findAllProjects();
            
         
            List<Project> userProjects = allProjects;
            if (userId != null && userRole != null) {
                boolean isTechnical = "DEVELOPER".equals(userRole) || 
                                     "SENIOR_IT_OFFICER".equals(userRole) || 
                                     "JUNIOR_IT_OFFICER".equals(userRole) || 
                                     "IT_GRADUATE_TRAINEE".equals(userRole);
                
                if (isTechnical) {
                    List<Task> allTasks = taskService.findAllTasks();
                    Set<Long> projectIds = new HashSet<>();
                    
                    for (Task task : allTasks) {
                        if (task.getAssignedTo() != null && 
                            task.getAssignedTo().getId() != null &&
                            task.getAssignedTo().getId().equals(userId) &&
                            task.getProject() != null) {
                            projectIds.add(task.getProject().getId());
                        }
                    }
                    
                    userProjects = allProjects.stream()
                        .filter(p -> projectIds.contains(p.getId()))
                        .collect(Collectors.toList());
                }
            }
            
            long green = 0, amber = 0, red = 0;
            for (Project p : userProjects) {
                if (p.getRagStatus() != null) {
                    if (p.getRagStatus() == Project.RagStatus.GREEN) green++;
                    else if (p.getRagStatus() == Project.RagStatus.AMBER) amber++;
                    else if (p.getRagStatus() == Project.RagStatus.RED) red++;
                }
            }
            
            report.put("total", userProjects.size());
            report.put("green", green);
            report.put("amber", amber);
            report.put("red", red);
            report.put("greenPercentage", userProjects.size() > 0 ? (green * 100) / userProjects.size() : 0);
            report.put("amberPercentage", userProjects.size() > 0 ? (amber * 100) / userProjects.size() : 0);
            report.put("redPercentage", userProjects.size() > 0 ? (red * 100) / userProjects.size() : 0);
            
            List<Project> criticalProjects = userProjects.stream()
                .filter(p -> p.getRagStatus() != null && 
                            (p.getRagStatus() == Project.RagStatus.RED || 
                             p.getRagStatus() == Project.RagStatus.AMBER))
                .collect(Collectors.toList());
            report.put("criticalProjects", criticalProjects);
            
        } catch (Exception e) {
            System.err.println("Error in RAG report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(report);
    }
    
    
    @GetMapping("/project-status")
    public ResponseEntity<List<Project>> getProjectStatusReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        System.out.println("=== CONTROLLER DATE FILTER ===");
        System.out.println("Received startDate: " + startDate);
        System.out.println("Received endDate: " + endDate);

        if (startDate != null && endDate != null) {
            return ResponseEntity.ok(projectService.findProjectsByDateRange(startDate, endDate));
        }

        return ResponseEntity.ok(projectService.findAllProjects());
    }

    
    @GetMapping("/developer-performance/{userId}")
    public ResponseEntity<Map<String, Object>> getDeveloperPerformanceReport(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
         if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();
        User user = new User();
        user.setId(userId);
        
        Map<String, Object> report = new HashMap<>();
        report.put("user", user);
        report.put("period", Map.of("startDate", startDate, "endDate", endDate));
        
        try {
            var updates = progressUpdateService.findUserProgressHistory(user, startDate, endDate);
            report.put("totalUpdates", updates.size());
            report.put("updatesWithBlockers", (int) updates.stream()
                .filter(u -> u.getBlockers() != null && !u.getBlockers().trim().isEmpty())
                .count());
            
            var tasks = taskService.findTasksByAssignedUser(user);
            report.put("totalTasks", tasks.size());
            report.put("completedTasks", (int) tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.COMPLETED)
                .count());
            report.put("pendingTasks", (int) tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.PENDING)
                .count());
            report.put("inProgressTasks", (int) tasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.IN_PROGRESS)
                .count());
            
            int productivityScore = 0;
            if (tasks.size() > 0) {
                long completed = tasks.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.COMPLETED)
                    .count();
                productivityScore = (int) ((completed * 100) / tasks.size());
            }
            report.put("productivityScore", productivityScore);
            
        } catch (Exception e) {
            System.err.println("Error in developer performance report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(report);
    }
    
    
    @GetMapping("/developers-performance")
    public ResponseEntity<List<Map<String, Object>>> getAllDevelopersPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Map<String, Object>> reports = new java.util.ArrayList<>();
        
        try {
            List<User> developers = userService.findAllTechnicalStaff();
            
            for (User developer : developers) {
                Map<String, Object> devReport = new HashMap<>();
                devReport.put("userId", developer.getId());
                devReport.put("username", developer.getUsername());
                devReport.put("fullName", developer.getFullName());
                devReport.put("role", developer.getRole());
                
                var tasks = taskService.findTasksByAssignedUser(developer);
                long completed = tasks.stream()
                    .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.COMPLETED)
                    .count();
                
                devReport.put("totalTasks", tasks.size());
                devReport.put("completedTasks", completed);
                devReport.put("pendingTasks", tasks.size() - completed);
                
                int completionRate = tasks.size() > 0 ? (int) ((completed * 100) / tasks.size()) : 0;
                devReport.put("completionRate", completionRate);
                
                var updates = progressUpdateService.findUserProgressHistory(developer, startDate, endDate);
                devReport.put("totalUpdates", updates.size());
                
                reports.add(devReport);
            }
            
        } catch (Exception e) {
            System.err.println("Error in developers performance report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(reports);
    }
    
   
    
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        
        LocalDate weekEnd = weekStart.plusDays(6);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", Map.of(
            "type", "weekly",
            "startDate", weekStart.toString(),
            "endDate", weekEnd.toString()
        ));
        
        try {
            var projects = projectService.findAllProjects();
            report.put("activeProjects", (int) projects.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == Project.ProjectStatus.IN_PROGRESS)
                .count());
            
            var allTasks = taskService.findAllTasks();
            report.put("tasksCompleted", (int) allTasks.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == com.nib.projecttracking.entity.Task.TaskStatus.COMPLETED)
                .count());
            
            report.put("progressUpdates", 0);
            
        } catch (Exception e) {
            System.err.println("Error in weekly report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(report);
    }
   
    
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlyReport(
            @RequestParam int year,
            @RequestParam int month) {
        
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        
        Map<String, Object> report = new HashMap<>();
        report.put("period", Map.of(
            "type", "monthly",
            "year", year,
            "month", month,
            "startDate", monthStart.toString(),
            "endDate", monthEnd.toString()
        ));
        
        try {
            report.put("totalProjects", projectService.findAllProjects().size());
            report.put("criticalProjects", projectService.findCriticalProjects().size());
            report.put("completedProjects", (int) projectService.findAllProjects().stream()
                .filter(p -> p.getStatus() != null && p.getStatus() == Project.ProjectStatus.COMPLETED)
                .count());
            
        } catch (Exception e) {
            System.err.println("Error in monthly report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(report);
    }
    
   
    
    @GetMapping("/api-lifecycle")
    public ResponseEntity<Map<String, Object>> getApiLifecycleReport() {
        Map<String, Object> report = new HashMap<>();
        
        try {
            var allApis = apiService.findAllApis();
            
            report.put("totalApis", allApis.size());
            
            Map<String, Long> apisByStage = new HashMap<>();
            apisByStage.put("DOCUMENTATION", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.DOCUMENTATION));
            apisByStage.put("DEVELOPMENT", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.DEVELOPMENT));
            apisByStage.put("TESTING", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.TESTING));
            apisByStage.put("SANDBOXING", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.SANDBOXING));
            apisByStage.put("UAT", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.UAT));
            apisByStage.put("PRODUCTION", apiService.countApisByStage(com.nib.projecttracking.entity.Api.ApiLifecycleStage.PRODUCTION));
            
            report.put("apisByStage", apisByStage);
            
        } catch (Exception e) {
            System.err.println("Error in API lifecycle report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ResponseEntity.ok(report);
    }
    

    
    /**
     * Get list of projects for report dropdown (role-based filtering)
     */
   @GetMapping("/projects/for-reports")
public ResponseEntity<List<Map<String, Object>>> getProjectsForReports(
        @RequestHeader(value = "X-User-Id", required = false) Long userId,
        @RequestHeader(value = "X-User-Role", required = false) String userRole) {
    try {
        List<Project> allProjects = projectService.findAllProjects();
        List<Project> filteredProjects = allProjects;
        
       
        if (userId != null && userRole != null) {
            
            if ("CEO".equals(userRole) || 
                "DEPUTY_CHIEF".equals(userRole) ||
                "DIRECTOR".equals(userRole) ||
                "DIGITAL_BANKING_MANAGER".equals(userRole) ||
                "QUALITY_ASSURANCE".equals(userRole)) {  
                filteredProjects = allProjects;
                System.out.println("👔 Executive/Digital Banking Manager - returning all " + allProjects.size() + " projects");
            }
            
            else if ("PROJECT_MANAGER".equals(userRole)) {
                User manager = new User();
                manager.setId(userId);
                filteredProjects = allProjects.stream()
                    .filter(p -> p.getManager() != null &&
                                p.getManager().getId() != null &&
                                p.getManager().getId().equals(userId))
                    .collect(Collectors.toList());
                System.out.println("👔 Manager - returning " + filteredProjects.size() + " managed projects");
            }
    
            else if (isTechnicalRole(userRole)) {
                List<Task> allTasks = taskService.findAllTasks();
                Set<Long> projectIds = new HashSet<>();
                for (Task task : allTasks) {
                    if (task.getAssignedTo() != null &&
                        task.getAssignedTo().getId() != null &&
                        task.getAssignedTo().getId().equals(userId) &&
                        task.getProject() != null) {
                        projectIds.add(task.getProject().getId());
                    }
                }
                filteredProjects = allProjects.stream()
                    .filter(p -> projectIds.contains(p.getId()))
                    .collect(Collectors.toList());
                System.out.println("🔧 Technical staff - returning " + filteredProjects.size() + " assigned projects");
            }
        }
        
        
        List<Map<String, Object>> projectSummaries = filteredProjects.stream()
            .map(p -> {
                Map<String, Object> summary = new HashMap<>();
                summary.put("id", p.getId());
                summary.put("name", p.getProjectName());
                summary.put("status", p.getStatus() != null ? p.getStatus().name() : null);
                summary.put("ragStatus", p.getRagStatus() != null ? p.getRagStatus().name() : null);
                return summary;
            })
            .sorted(Comparator.comparing(p -> (String) p.get("name")))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(projectSummaries);
        
    } catch (Exception e) {
        System.err.println("Error fetching projects for reports: " + e.getMessage());
        return ResponseEntity.badRequest().body(new ArrayList<>());
    }
}
    
    
    /**
     * Get detailed project report with all components (milestones, tasks, updates, attachments)
     */
    @GetMapping("/project/{projectId}/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedProjectReport(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {
        
        try {
            
            Project project = projectService.findProjectById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
            
            
            Map<String, Object> summary = buildReportSummary(project, startDate, endDate);
            
         
            List<Map<String, Object>> milestones = milestoneService.findByProjectId(projectId).stream()
                .map(this::mapMilestoneToDTO)
                .collect(Collectors.toList());
            
            
            List<Map<String, Object>> tasks = taskService.findTasksByProjectId(projectId).stream()
                .map(task -> mapTaskToDTO(task, startDate, endDate))
                .collect(Collectors.toList());
            
            List<Map<String, Object>> updates = progressUpdateService
                .findByProjectIdAndDateRange(projectId, startDate, endDate).stream()
                .map(this::mapProgressUpdateToDTO)
                .collect(Collectors.toList());
            
          
            List<Map<String, Object>> attachments = projectService.findProjectAttachments(projectId).stream()
                .limit(10)
                .map(this::mapAttachmentToDTO)
                .collect(Collectors.toList());
            
            
            Map<String, Object> report = new HashMap<>();
            report.put("projectId", project.getId());
            report.put("projectName", project.getProjectName());
            report.put("description", project.getDescription());
            report.put("status", project.getStatus() != null ? project.getStatus().name() : null);
            report.put("ragStatus", project.getRagStatus() != null ? project.getRagStatus().name() : null);
            report.put("startDate", project.getStartDate());
            report.put("endDate", project.getEndDate());
            report.put("managerName", project.getManager() != null ? project.getManager().getFullName() : "Unassigned");
            report.put("summary", summary);
            report.put("milestones", milestones);
            report.put("tasks", tasks);
            report.put("dailyUpdates", updates);  
            report.put("attachments", attachments);
            report.put("totalAttachments", projectService.findProjectAttachments(projectId).size());
            report.put("reportGeneratedAt", java.time.LocalDateTime.now());
            report.put("reportPeriodStart", startDate);
            report.put("reportPeriodEnd", endDate);
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            System.err.println("Error generating detailed report: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to generate detailed report",
                "message", e.getMessage()
            ));
        }
    }
    
    
    
    /**
     * Export detailed project report as PDF
     */
    @GetMapping("/project/{projectId}/export/pdf")
    public ResponseEntity<byte[]> exportDetailedReportPdf(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {
        
        try {
           
            ResponseEntity<Map<String, Object>> reportResponse = 
                getDetailedProjectReport(projectId, startDate, endDate, userId);
            
            if (reportResponse.getStatusCode().is2xxSuccessful() && reportResponse.getBody() != null) {
                Map<String, Object> report = reportResponse.getBody();
                
                
                byte[] pdfContent = generatePdfContent(report);
                
                return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", 
                        "attachment; filename=\"project-" + projectId + "-detailed-report-" + 
                        startDate + "-to-" + endDate + ".pdf\"")
                    .body(pdfContent);
            }
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            System.err.println("Error exporting PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
@SuppressWarnings("unchecked")
private byte[] generatePdfContent(Map<String, Object> report) {
    try {
        StringBuilder pdfContent = new StringBuilder();
        
     
        pdfContent.append("%PDF-1.4\n");
        pdfContent.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        pdfContent.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        pdfContent.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
        pdfContent.append("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");
        
        
        String projectName = report.get("projectName") != null ? report.get("projectName").toString() : "Unknown Project";
        String description = report.get("description") != null ? report.get("description").toString() : "";
        String status = report.get("status") != null ? report.get("status").toString() : "N/A";
        String ragStatus = report.get("ragStatus") != null ? report.get("ragStatus").toString() : "N/A";
        String managerName = report.get("managerName") != null ? report.get("managerName").toString() : "Unassigned";
        
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        List<Map<String, Object>> milestones = (List<Map<String, Object>>) report.get("milestones");
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) report.get("tasks");
        List<Map<String, Object>> updates = (List<Map<String, Object>>) report.get("dailyUpdates");
        
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 16 Tf 50 750 Td (Project Report: ").append(escapePdf(projectName)).append(") Tj ET\n");
        if (!description.isEmpty()) {
            content.append("BT /F1 10 Tf 50 720 Td (").append(escapePdf(description)).append(") Tj ET\n");
        }
        content.append("BT /F1 10 Tf 50 ").append(description.isEmpty() ? "720" : "700").append(" Td (Manager: ").append(escapePdf(managerName)).append(") Tj ET\n");
        content.append("BT /F1 10 Tf 50 ").append(description.isEmpty() ? "705" : "685").append(" Td (Status: ").append(escapePdf(status)).append(" | RAG: ").append(escapePdf(ragStatus)).append(") Tj ET\n");
        content.append("BT /F1 10 Tf 50 660 Td (Summary:) Tj ET\n");
        
        if (summary != null) {
            content.append("BT /F1 9 Tf 70 645 Td (Total Tasks: ").append(summary.get("totalTasks")).append(") Tj ET\n");
            content.append("BT /F1 9 Tf 70 632 Td (Completed: ").append(summary.get("completedTasks")).append(") Tj ET\n");
            content.append("BT /F1 9 Tf 70 619 Td (Completion: ").append(summary.get("completionPercentage")).append("%) Tj ET\n");
            content.append("BT /F1 9 Tf 70 606 Td (Updates: ").append(summary.get("totalUpdates")).append(") Tj ET\n");
        }
        
        int yPos = 575;
        content.append("BT /F1 10 Tf 50 ").append(yPos).append(" Td (Milestones:) Tj ET\n");
        yPos -= 15;
        if (milestones != null && !milestones.isEmpty()) {
            for (Map<String, Object> m : milestones) {
                if (yPos < 100) break;
                String mName = m.get("name") != null ? m.get("name").toString() : "Unknown";
                String mStatus = m.get("status") != null ? m.get("status").toString() : "N/A";
                content.append("BT /F1 9 Tf 70 ").append(yPos).append(" Td (- ").append(escapePdf(mName))
                    .append(" (").append(escapePdf(mStatus)).append(")) Tj ET\n");
                yPos -= 13;
            }
        } else {
            content.append("BT /F1 9 Tf 70 ").append(yPos).append(" Td (No milestones) Tj ET\n");
        }
        
        yPos -= 20;
        content.append("BT /F1 10 Tf 50 ").append(yPos).append(" Td (Tasks:) Tj ET\n");
        yPos -= 15;
        if (tasks != null && !tasks.isEmpty()) {
            for (Map<String, Object> t : tasks) {
                if (yPos < 200) break;
                String tName = t.get("taskName") != null ? t.get("taskName").toString() : "Unknown";
                String tStatus = t.get("status") != null ? t.get("status").toString() : "N/A";
                content.append("BT /F1 9 Tf 70 ").append(yPos).append(" Td (- ").append(escapePdf(tName))
                    .append(" (").append(escapePdf(tStatus)).append(")) Tj ET\n");
                yPos -= 13;
            }
        } else {
            content.append("BT /F1 9 Tf 70 ").append(yPos).append(" Td (No tasks) Tj ET\n");
        }
        
        yPos -= 20;
        content.append("BT /F1 10 Tf 50 ").append(yPos).append(" Td (Progress Updates:) Tj ET\n");
        yPos -= 15;
        if (updates != null && !updates.isEmpty()) {
            for (Map<String, Object> u : updates) {
                if (yPos < 50) break;
                String uDate = u.get("updateDate") != null ? u.get("updateDate").toString() : "";
                String uContent = u.get("content") != null ? u.get("content").toString() : "";
                content.append("BT /F1 8 Tf 70 ").append(yPos).append(" Td (").append(escapePdf(uDate))
                    .append(": ").append(escapePdf(uContent)).append(") Tj ET\n");
                yPos -= 11;
            }
        } else {
            content.append("BT /F1 9 Tf 70 ").append(yPos).append(" Td (No progress updates in this period) Tj ET\n");
        }
        
       
        int contentLength = content.length();
        pdfContent.append("5 0 obj\n<< /Length ").append(contentLength).append(" >>\nstream\n");
        pdfContent.append(content);
        pdfContent.append("endstream\nendobj\n");
        
        
        int xrefOffset = 500 + contentLength;
        pdfContent.append("xref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000250 00000 n \n0000000320 00000 n \n");
        pdfContent.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xrefOffset).append("\n%%EOF");
        
        return pdfContent.toString().getBytes();
        
    } catch (Exception e) {
        System.err.println("Error generating PDF: " + e.getMessage());
        e.printStackTrace();
       
        String errorPdf = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n5 0 obj\n<< /Length 44 >>\nstream\nBT /F1 12 Tf 50 750 Td (Error generating PDF report) Tj ET\nendstream\nendobj\nxref\n0 6\n0000000000 65535 f \n0000000009 00000 n \n0000000058 00000 n \n0000000115 00000 n \n0000000250 00000 n \n0000000320 00000 n \ntrailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n414\n%%EOF";
        return errorPdf.getBytes();
    }
}
    /**
     * Escape special characters for PDF content stream
     */
    private String escapePdf(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("(", "\\(")
                  .replace(")", "\\)")
                  .replace("\n", "\\n");
    }
    
    /**
     * Export detailed project report as Excel
     */
    @GetMapping("/project/{projectId}/export/excel")
    public ResponseEntity<byte[]> exportDetailedReportExcel(
            @PathVariable Long projectId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long userId) {
        
        try {
           
            ResponseEntity<Map<String, Object>> reportResponse = 
                getDetailedProjectReport(projectId, startDate, endDate, userId);
            
            if (reportResponse.getStatusCode().is2xxSuccessful() && reportResponse.getBody() != null) {
                Map<String, Object> report = reportResponse.getBody();
                
               
                byte[] excelContent = generateExcelContent(report);
                
                return ResponseEntity.ok()
                    .header("Content-Type", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", 
                        "attachment; filename=\"project-" + projectId + "-detailed-report-" + 
                        startDate + "-to-" + endDate + ".xlsx\"")
                    .body(excelContent);
            }
            
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            System.err.println("Error exporting Excel: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
   /**
 * Generate Excel content from report data (CSV format)
 */
@SuppressWarnings("unchecked")
private byte[] generateExcelContent(Map<String, Object> report) {
    try {
        StringBuilder csv = new StringBuilder();
        
       
        csv.append("Project Report\n");
        csv.append("Project Name,").append(escapeCsv(report.get("projectName") != null ? report.get("projectName").toString() : "")).append("\n");
        csv.append("Description,").append(escapeCsv(report.get("description") != null ? report.get("description").toString() : "")).append("\n");
        csv.append("Manager,").append(escapeCsv(report.get("managerName") != null ? report.get("managerName").toString() : "")).append("\n");
        csv.append("Status,").append(escapeCsv(report.get("status") != null ? report.get("status").toString() : "")).append("\n");
        csv.append("RAG Status,").append(escapeCsv(report.get("ragStatus") != null ? report.get("ragStatus").toString() : "")).append("\n\n");
        
        
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        if (summary != null) {
            csv.append("Summary\n");
            csv.append("Total Tasks,").append(summary.get("totalTasks")).append("\n");
            csv.append("Completed Tasks,").append(summary.get("completedTasks")).append("\n");
            csv.append("Completion %,").append(summary.get("completionPercentage")).append("\n");
            csv.append("Total Updates,").append(summary.get("totalUpdates")).append("\n");
            csv.append("Updates with Blockers,").append(summary.get("updatesWithBlockers")).append("\n\n");
        }
        
       
        csv.append("Milestones\n");
        csv.append("Name,Status,Target Date,Completed Date\n");
        List<Map<String, Object>> milestones = (List<Map<String, Object>>) report.get("milestones");
        if (milestones != null) {
            for (Map<String, Object> m : milestones) {
                csv.append(escapeCsv(m.get("name") != null ? m.get("name").toString() : ""))
                   .append(",")
                   .append(escapeCsv(m.get("status") != null ? m.get("status").toString() : ""))
                   .append(",")
                   .append(m.get("targetDate") != null ? m.get("targetDate") : "")
                   .append(",")
                   .append(m.get("completedDate") != null ? m.get("completedDate") : "")
                   .append("\n");
            }
        }
        csv.append("\n");
        
       
        csv.append("Tasks\n");
        csv.append("Task Name,Status,Assigned To,Priority,Due Date,Completion %\n");
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) report.get("tasks");
        if (tasks != null) {
            for (Map<String, Object> t : tasks) {
                csv.append(escapeCsv(t.get("taskName") != null ? t.get("taskName").toString() : ""))
                   .append(",")
                   .append(escapeCsv(t.get("status") != null ? t.get("status").toString() : ""))
                   .append(",")
                   .append(escapeCsv(t.get("assignedToName") != null ? t.get("assignedToName").toString() : ""))
                   .append(",")
                   .append(t.get("priorityLabel") != null ? t.get("priorityLabel") : "")
                   .append(",")
                   .append(t.get("dueDate") != null ? t.get("dueDate") : "")
                   .append(",")
                   .append(t.get("completionPercentage") != null ? t.get("completionPercentage") : "")
                   .append("\n");
            }
        }
        csv.append("\n");
        
     
        csv.append("Progress Updates\n");
        csv.append("Date,Content,Blockers,Next Steps,Submitted By\n");
        List<Map<String, Object>> updates = (List<Map<String, Object>>) report.get("dailyUpdates");
        if (updates != null) {
            for (Map<String, Object> u : updates) {
                csv.append(u.get("updateDate") != null ? u.get("updateDate") : "")
                   .append(",")
                   .append(escapeCsv(u.get("content") != null ? u.get("content").toString() : ""))
                   .append(",")
                   .append(escapeCsv(u.get("blockers") != null ? u.get("blockers").toString() : ""))
                   .append(",")
                   .append(escapeCsv(u.get("nextSteps") != null ? u.get("nextSteps").toString() : ""))
                   .append(",")
                   .append(escapeCsv(u.get("submittedByName") != null ? u.get("submittedByName").toString() : ""))
                   .append("\n");
            }
        }
        
        return csv.toString().getBytes();
        
    } catch (Exception e) {
        System.err.println("Error generating Excel: " + e.getMessage());
        e.printStackTrace();
        return "Error generating Excel file".getBytes();
    }
}
    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String text) {
        if (text == null) return "";
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
    

    
   private boolean isBusinessRole(String userRole) {
    return "BUSINESS".equals(userRole);
}

private boolean isTechnicalRole(String userRole) {
    return "DEVELOPER".equals(userRole) || 
           "SENIOR_IT_OFFICER".equals(userRole) || 
           "JUNIOR_IT_OFFICER".equals(userRole) || 
           "IT_GRADUATE_TRAINEE".equals(userRole);
}
    
    private Map<String, Object> buildReportSummary(Project project, LocalDate start, LocalDate end) {
        Map<String, Object> summary = new HashMap<>();
        
        List<Task> tasks = project.getTasks() != null ? project.getTasks() : new ArrayList<>();
        
        long completed = tasks.stream()
            .filter(t -> t.getStatus() != null && Task.TaskStatus.COMPLETED.equals(t.getStatus()))
            .count();
        
        long pending = tasks.stream()
            .filter(t -> t.getStatus() != null && Task.TaskStatus.PENDING.equals(t.getStatus()))
            .count();
        
        double completionPct = tasks.isEmpty() ? 0 : Math.round((double) completed / tasks.size() * 100);
        
        long updatesCount = progressUpdateService.findByProjectIdAndDateRange(project.getId(), start, end).size();
        long updatesWithBlockers = progressUpdateService.findByProjectIdAndDateRange(project.getId(), start, end).stream()
            .filter(u -> u.getBlockers() != null && !u.getBlockers().trim().isEmpty())
            .count();
        
        List<com.nib.projecttracking.entity.Milestone> milestones = 
            project.getMilestones() != null ? project.getMilestones() : new ArrayList<>();
        long completedMilestones = milestones.stream()
            .filter(m -> m.getStatus() != null && 
                com.nib.projecttracking.entity.Milestone.MilestoneStatus.COMPLETED.equals(m.getStatus()))
            .count();
        
        summary.put("totalTasks", tasks.size());
        summary.put("completedTasks", (int) completed);
        summary.put("pendingTasks", (int) pending);
        summary.put("completionPercentage", completionPct);
        summary.put("totalUpdates", (int) updatesCount);
        summary.put("updatesWithBlockers", (int) updatesWithBlockers);
        summary.put("milestonesTotal", milestones.size());
        summary.put("milestonesCompleted", (int) completedMilestones);
        summary.put("totalAttachments", project.getAttachments() != null ? project.getAttachments().size() : 0);
        
        if (project.getEndDate() != null) {
            summary.put("daysUntilDeadline", 
                (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate()));
        }
        
        return summary;
    }
    

    private Map<String, Object> mapMilestoneToDTO(com.nib.projecttracking.entity.Milestone m) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", m.getId());
        dto.put("name", m.getMilestoneName());
        dto.put("description", m.getDescription());
        dto.put("targetDate", m.getTargetDate());
        dto.put("completedDate", m.getActualDate());
        dto.put("status", m.getStatus() != null ? m.getStatus().name() : null);
        
        if (m.getTargetDate() != null) {
            dto.put("daysUntilTarget", 
                (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), m.getTargetDate()));
            dto.put("isOverdue", m.getStatus() != null && 
                !com.nib.projecttracking.entity.Milestone.MilestoneStatus.COMPLETED.equals(m.getStatus()) &&
                m.getTargetDate().isBefore(LocalDate.now()));
        }
        
        return dto;
    }
    
    
    private Map<String, Object> mapTaskToDTO(Task task, LocalDate start, LocalDate end) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", task.getId());
        dto.put("taskName", task.getTaskName());
        dto.put("description", task.getDescription());
        dto.put("status", task.getStatus() != null ? task.getStatus().name() : null);
        dto.put("priority", task.getPriority());
        dto.put("priorityLabel", getPriorityLabel(task.getPriority()));
        dto.put("dueDate", task.getDueDate());
        dto.put("completionPercentage", task.getCompletionPercentage());
        dto.put("assignedToName", task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Unassigned");
        dto.put("assignedToRole", task.getAssignedTo() != null ? task.getAssignedTo().getRole().name() : null);
        
      
        List<Map<String, Object>> updates = progressUpdateService
            .findByUserIdAndDateRange(
                task.getAssignedTo() != null ? task.getAssignedTo().getId() : 0L, 
                start, end).stream()
            .map(this::mapProgressUpdateToDTO)
            .collect(Collectors.toList());
        dto.put("updates", updates);
        
        if (task.getDueDate() != null) {
            dto.put("daysUntilDue", 
                (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), task.getDueDate()));
            dto.put("isOverdue", task.getStatus() != null && 
                !Task.TaskStatus.COMPLETED.equals(task.getStatus()) &&
                task.getDueDate().isBefore(LocalDate.now()));
        }
        
        dto.put("hasBlockers", updates.stream()
            .anyMatch(u -> u.get("blockers") != null && 
                          !((String) u.get("blockers")).trim().isEmpty()));
        
        return dto;
    }
    
    private Map<String, Object> mapProgressUpdateToDTO(com.nib.projecttracking.entity.ProgressUpdate pu) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", pu.getId());
        dto.put("content", pu.getCompletedWork());
        dto.put("blockers", pu.getBlockers());
        dto.put("nextSteps", pu.getOngoingWork());
        dto.put("updateDate", pu.getUpdateDate());
        dto.put("submittedAt", pu.getCreatedAt());
        dto.put("submittedByName", pu.getUser() != null ? pu.getUser().getFullName() : "Unknown");
        dto.put("submittedByRole", pu.getUser() != null ? pu.getUser().getRole().name() : null);
        return dto;
    }
    
    
    private Map<String, Object> mapAttachmentToDTO(com.nib.projecttracking.entity.Attachment a) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", a.getId());
        dto.put("fileName", a.getOriginalFileName());
        dto.put("fileType", a.getFileType());
        dto.put("fileSize", a.getFileSize());
        dto.put("uploadedAt", a.getUploadedAt());
        dto.put("uploadedByName", a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : "Unknown");
        dto.put("description", a.getDescription());
        return dto;
    }
    

    private String getPriorityLabel(Integer priority) {
        if (priority == null) return "Medium";
        return switch (priority) {
            case 4 -> "Critical";
            case 3 -> "High";
            case 2 -> "Medium";
            case 1 -> "Low";
            default -> "Medium";
        };
    }
}