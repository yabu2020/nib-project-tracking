package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.*;
import com.nib.projecttracking.repository.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional(readOnly = true)
public class ReportService {
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private MilestoneRepository milestoneRepository;
    
    @Autowired
    private ProgressUpdateRepository progressUpdateRepository;
    
    @Autowired
    private AttachmentRepository attachmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ReportExportService reportExportService;
    
   
    
    public DetailedProjectReportDTO generateDetailedProjectReport(
            Long projectId, 
            LocalDate startDate, 
            LocalDate endDate,
            Long requestingUserId) {
        
      
        Project project = projectRepository.findProjectWithDetails(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        
        DetailedProjectReportDTO.ReportSummary summary = buildReportSummary(project, startDate, endDate);
        
        List<Milestone> milestones = milestoneRepository.findMilestonesByDateRange(projectId, startDate, endDate);
        List<DetailedProjectReportDTO.MilestoneDTO> milestoneDTOs = milestones.stream()
            .map(this::mapMilestoneToDTO)
            .collect(Collectors.toList());
        
      
        List<Task> tasks = taskRepository.findTasksByProjectId(projectId);
        List<DetailedProjectReportDTO.TaskDetailDTO> taskDTOs = tasks.stream()
            .map(task -> mapTaskToDTO(task, startDate, endDate))
            .collect(Collectors.toList());
        
       
        List<ProgressUpdate> progressUpdates = progressUpdateRepository
            .findUpdatesByProjectAndDateRange(projectId, startDate, endDate);
        
        List<DetailedProjectReportDTO.ProgressUpdateDTO> allUpdates = progressUpdates.stream()
            .sorted(Comparator.comparing(ProgressUpdate::getCreatedAt).reversed())
            .map(this::mapProgressUpdateToDTO)
            .collect(Collectors.toList());
        
       
        List<Attachment> attachments = attachmentRepository.findByProjectId(projectId);
        List<DetailedProjectReportDTO.AttachmentSummaryDTO> attachmentSummaries = attachments.stream()
            .limit(10)
            .map(this::mapAttachmentToSummaryDTO)
            .collect(Collectors.toList());
     
        return DetailedProjectReportDTO.builder()
            .projectId(project.getId())
            .projectName(project.getProjectName())
            .description(project.getDescription())
            .projectType(project.getProjectType())
            .status(project.getStatus() != null ? project.getStatus().name() : null)
            .ragStatus(project.getRagStatus() != null ? project.getRagStatus().name() : null)
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .actualEndDate(project.getActualEndDate())
            .completionPercentage(project.getCompletionPercentage())
            .managerName(project.getManager() != null ? project.getManager().getFullName() : "Unassigned")
            .initiatedByName(project.getInitiatedBy() != null ? project.getInitiatedBy().getFullName() : "Unknown")
            .createdAt(project.getCreatedAt())
            .updatedAt(project.getUpdatedAt())
            .summary(summary)
            .milestones(milestoneDTOs)
            .tasks(taskDTOs)
            .dailyUpdates(allUpdates)
            .attachments(attachmentSummaries)
            .totalAttachments(attachments.size())
            .reportGeneratedAt(LocalDateTime.now())
            .reportPeriodStart(startDate)
            .reportPeriodEnd(endDate)
            .build();
    }
    
    private DetailedProjectReportDTO.ReportSummary buildReportSummary(Project project, LocalDate start, LocalDate end) {
        List<Task> tasks = project.getTasks() != null ? project.getTasks() : new ArrayList<>();
        
        long completed = tasks.stream().filter(t -> t.getStatus() != null && Task.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        long pending = tasks.stream().filter(t -> t.getStatus() != null && Task.TaskStatus.PENDING.equals(t.getStatus())).count();
        long inProgress = tasks.stream().filter(t -> t.getStatus() != null && Task.TaskStatus.IN_PROGRESS.equals(t.getStatus())).count();
        long blocked = tasks.stream().filter(t -> t.getStatus() != null && Task.TaskStatus.BLOCKED.equals(t.getStatus())).count();
        long overdue = tasks.stream()
            .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) 
                && !Task.TaskStatus.COMPLETED.equals(t.getStatus() != null ? t.getStatus() : Task.TaskStatus.PENDING))
            .count();
        
        double completionPct = tasks.isEmpty() ? 0 : Math.round((double) completed / tasks.size() * 100);
        
     
        
  
        long totalUpdates = progressUpdateRepository.countByProjectIdAndDateRange(project.getId(), start, end);
 
long updatesWithBlockers = progressUpdateRepository.countActiveBlockersByProjectAndDateRange(
    project.getId(), start, end);        
        List<Milestone> milestones = project.getMilestones() != null ? project.getMilestones() : new ArrayList<>();
        long completedMilestones = milestones.stream()
            .filter(m -> m.getStatus() != null && Milestone.MilestoneStatus.COMPLETED.equals(m.getStatus()))
            .count();
        long overdueMilestones = milestones.stream()
            .filter(m -> m.getTargetDate() != null && m.getTargetDate().isBefore(LocalDate.now())
                && !Milestone.MilestoneStatus.COMPLETED.equals(m.getStatus() != null ? m.getStatus() : Milestone.MilestoneStatus.PLANNED))
            .count();
        
        long attachmentCount = Optional.ofNullable(project.getAttachments()).orElse(new ArrayList<>()).size();
        
        return DetailedProjectReportDTO.ReportSummary.builder()
            .totalTasks(tasks.size())
            .completedTasks((int) completed)
            .pendingTasks((int) pending)
            .inProgressTasks((int) inProgress)
            .blockedTasks((int) blocked)
            .overdueTasks((int) overdue)
            .completionPercentage(completionPct)
            .totalUpdates((int) totalUpdates)
            .updatesWithBlockers((int) updatesWithBlockers)
            .milestonesTotal(milestones.size())
            .milestonesCompleted((int) completedMilestones)
            .overdueMilestones((int) overdueMilestones)
            .totalAttachments((int) attachmentCount)
            .daysUntilDeadline(project.getEndDate() != null ? 
                (int) ChronoUnit.DAYS.between(LocalDate.now(), project.getEndDate()) : null)
            .build();
    }
    
    private DetailedProjectReportDTO.MilestoneDTO mapMilestoneToDTO(Milestone m) {
        LocalDate today = LocalDate.now();
        LocalDate target = m.getTargetDate();
        boolean isOverdue = m.getStatus() != null && !Milestone.MilestoneStatus.COMPLETED.equals(m.getStatus()) 
            && target != null && target.isBefore(today);
        int daysUntil = target != null ? (int) ChronoUnit.DAYS.between(today, target) : -1;
        
        return DetailedProjectReportDTO.MilestoneDTO.builder()
            .id(m.getId())
            .name(m.getMilestoneName())
            .description(m.getDescription())
            .targetDate(m.getTargetDate())
            .completedDate(m.getActualDate()) 
            .status(m.getStatus() != null ? m.getStatus().name() : null)
            .daysUntilTarget(daysUntil)
            .isOverdue(isOverdue)
            .build();
    }
    

    private DetailedProjectReportDTO.TaskDetailDTO mapTaskToDTO(Task task, LocalDate start, LocalDate end) {
        List<DetailedProjectReportDTO.ProgressUpdateDTO> updates = new ArrayList<>();
        
        
        boolean hasBlockers = false;
        
        LocalDate due = task.getDueDate();
        LocalDate today = LocalDate.now();
        boolean isOverdue = task.getStatus() != null && !Task.TaskStatus.COMPLETED.equals(task.getStatus()) 
            && due != null && due.isBefore(today);
        int daysUntilDue = due != null ? (int) ChronoUnit.DAYS.between(today, due) : -1;
        
        return DetailedProjectReportDTO.TaskDetailDTO.builder()
            .id(task.getId())
            .taskName(task.getTaskName())
            .description(task.getDescription())
            .status(task.getStatus() != null ? task.getStatus().name() : null)
            .priority(task.getPriority())
            .priorityLabel(getPriorityLabel(task.getPriority()))
            .dueDate(task.getDueDate())
            .completionPercentage(task.getCompletionPercentage())
            .assignedToName(task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : "Unassigned")
            .assignedToRole(task.getAssignedTo() != null ? task.getAssignedTo().getRole().name() : null)
            .initiatedByName(task.getAssignedBy() != null ? task.getAssignedBy().getFullName() : "Unknown")
            .createdAt(task.getCreatedAt())
            .updatedAt(task.getUpdatedAt())
            .updates(updates)  
            .daysUntilDue(daysUntilDue)
            .isOverdue(isOverdue)
            .hasBlockers(hasBlockers)
            .attachmentCount(Optional.ofNullable(task.getAttachments()).orElse(new ArrayList<>()).size())
            .build();
    }
    
 private DetailedProjectReportDTO.ProgressUpdateDTO mapProgressUpdateToDTO(ProgressUpdate pu) {
    return DetailedProjectReportDTO.ProgressUpdateDTO.builder()
        .id(pu.getId())
        .content(pu.getCompletedWork())
        .blockers(pu.getBlockers())
        .blockerStatus(pu.getBlockerStatus())
        .solvedBy(pu.getSolvedById() != null ? 
            userRepository.findById(pu.getSolvedById())
                .map(User::getFullName)
                .orElse(null) : null)  
        .solvedAt(pu.getSolvedAt())
        .nextSteps(pu.getOngoingWork())
        .progressPercentage(null)
        .updateDate(pu.getUpdateDate())
        .submittedAt(pu.getCreatedAt())
        .submittedByName(pu.getUser() != null ? pu.getUser().getFullName() : "Unknown")
        .submittedByRole(pu.getUser() != null ? pu.getUser().getRole().name() : null)
        .taskId(null)
        .projectLevel(true)
        .build();
}
    
    private DetailedProjectReportDTO.AttachmentSummaryDTO mapAttachmentToSummaryDTO(Attachment a) {
        return DetailedProjectReportDTO.AttachmentSummaryDTO.builder()
            .id(a.getId())
            .fileName(a.getOriginalFileName())
            .fileType(a.getFileType())
            .fileSize(a.getFileSize())
            .uploadedAt(a.getUploadedAt())
            .uploadedByName(a.getUploadedBy() != null ? a.getUploadedBy().getFullName() : "Unknown")
            .description(a.getDescription())
            .build();
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
    
    public byte[] exportDetailedReportPdf(Long projectId, LocalDate startDate, LocalDate endDate, Long userId) {
        DetailedProjectReportDTO report = generateDetailedProjectReport(projectId, startDate, endDate, userId);
        return reportExportService.generateDetailedProjectPdf(report);
    }
    
    public byte[] exportDetailedReportExcel(Long projectId, LocalDate startDate, LocalDate endDate, Long userId) {
        DetailedProjectReportDTO report = generateDetailedProjectReport(projectId, startDate, endDate, userId);
        return reportExportService.generateDetailedProjectExcel(report);
    }
    
    public List<ProjectSummaryDTO> getProjectSummariesForReports(Long userId, String userRole) {
        List<Project> projects;
        
        if ("CEO".equals(userRole) || "DEPUTY_CHIEF".equals(userRole)) {
            projects = projectRepository.findAll();
        } else if (Arrays.asList("PROJECT_MANAGER", "CORE_BANKING_MANAGER", "DIGITAL_BANKING_MANAGER").contains(userRole)) {
            User manager = userRepository.findById(userId).orElse(null);
            projects = manager != null ? projectRepository.findByManager(manager) : new ArrayList<>();
        } else {
            projects = projectRepository.findProjectsByUserHasTasks(userId);
        }
        
        return projects.stream()
            .map(p -> ProjectSummaryDTO.builder()
                .id(p.getId())
                .name(p.getProjectName())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .ragStatus(p.getRagStatus() != null ? p.getRagStatus().name() : null)
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .managerName(p.getManager() != null ? p.getManager().getFullName() : null)
                .build())
            .sorted(Comparator.comparing(ProjectSummaryDTO::getName))
            .collect(Collectors.toList());
    }
    
    public ProjectQuickStatsDTO getProjectQuickStats(Long projectId, LocalDate startDate, LocalDate endDate) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        List<Task> tasks = Optional.ofNullable(project.getTasks()).orElse(new ArrayList<>());
        long completed = tasks.stream().filter(t -> t.getStatus() != null && Task.TaskStatus.COMPLETED.equals(t.getStatus())).count();
        double completionPct = tasks.isEmpty() ? 0 : Math.round((double) completed / tasks.size() * 100);
        
        long updatesInRange = progressUpdateRepository.countByProjectIdAndDateRange(projectId, startDate, endDate);
        
        return ProjectQuickStatsDTO.builder()
            .projectId(projectId)
            .projectName(project.getProjectName())
            .ragStatus(project.getRagStatus() != null ? project.getRagStatus().name() : null)
            .completionPercentage(completionPct)
            .totalTasks(tasks.size())
            .completedTasks((int) completed)
            .recentUpdates((int) updatesInRange)
            .overdueTasks((int) tasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now())
                    && !Task.TaskStatus.COMPLETED.equals(t.getStatus() != null ? t.getStatus() : Task.TaskStatus.PENDING))
                .count())
            .build();
    }
    
   
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailedProjectReportDTO {
        private Long projectId;
        private String projectName;
        private String description;
        private String projectType;
        private String status;
        private String ragStatus;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate actualEndDate;
        private Integer completionPercentage;
        private String managerName;
        private String initiatedByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private ReportSummary summary;
        private List<MilestoneDTO> milestones;
        private List<TaskDetailDTO> tasks;
        private List<ProgressUpdateDTO> dailyUpdates;
        private List<AttachmentSummaryDTO> attachments;
        private Integer totalAttachments;
        private LocalDateTime reportGeneratedAt;
        private LocalDate reportPeriodStart;
        private LocalDate reportPeriodEnd;
        
        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class ReportSummary {
            private Integer totalTasks;
            private Integer completedTasks;
            private Integer pendingTasks;
            private Integer inProgressTasks;
            private Integer blockedTasks;
            private Integer overdueTasks;
            private Double completionPercentage;
            private Integer totalUpdates;
            private Integer updatesWithBlockers;
            private Integer milestonesTotal;
            private Integer milestonesCompleted;
            private Integer overdueMilestones;
            private Integer totalAttachments;
            private Integer daysUntilDeadline;
        }
        
        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class MilestoneDTO {
            private Long id;
            private String name;
            private String description;
            private LocalDate targetDate;
            private LocalDate completedDate;
            private String status;
            private Integer daysUntilTarget;
            private Boolean isOverdue;
        }
        
        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class TaskDetailDTO {
            private Long id;
            private String taskName;
            private String description;
            private String status;
            private Integer priority;
            private String priorityLabel;
            private LocalDate dueDate;
            private Integer completionPercentage;
            private String assignedToName;
            private String assignedToRole;
            private String initiatedByName;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;
            private List<ProgressUpdateDTO> updates;
            private Integer daysUntilDue;
            private Boolean isOverdue;
            private Boolean hasBlockers;
            private Integer attachmentCount;
        }
        
        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class ProgressUpdateDTO {
            private Long id;
            private String content;
            private String blockers;
            private String blockerStatus;     
            private String solvedBy;          
            private LocalDateTime solvedAt;    
            private String nextSteps;
            private Integer progressPercentage;
            private LocalDate updateDate;
            private LocalDateTime submittedAt;
            private String submittedByName;
            private String submittedByRole;
            private Long taskId;
            private Boolean projectLevel;
        }
        
        @Data @Builder @NoArgsConstructor @AllArgsConstructor
        public static class AttachmentSummaryDTO {
            private Long id;
            private String fileName;
            private String fileType;
            private Long fileSize;
            private LocalDateTime uploadedAt;
            private String uploadedByName;
            private String description;
        }
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectSummaryDTO {
        private Long id;
        private String name;
        private String status;
        private String ragStatus;
        private LocalDate startDate;
        private LocalDate endDate;
        private String managerName;
    }
    
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ProjectQuickStatsDTO {
        private Long projectId;
        private String projectName;
        private String ragStatus;
        private Double completionPercentage;
        private Integer totalTasks;
        private Integer completedTasks;
        private Integer recentUpdates;
        private Integer overdueTasks;
    }
}