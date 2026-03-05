package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Attachment;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.Task;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.AttachmentRepository;
import com.nib.projecttracking.repository.ProjectRepository;
import com.nib.projecttracking.repository.TaskRepository;
import com.nib.projecttracking.repository.UserRepository;  
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AttachmentService {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogService activityLogService;

    private final String UPLOAD_DIR = "uploads/";

    /**
     * Upload file to project
     */
    public Attachment uploadToProject(Long projectId, Long userId, MultipartFile file, String description)
            throws IOException {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

       
        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Attachment attachment = saveFile(file, uploader, project, null, description);

        String details = String.format("Uploaded file '%s' (%s) to project '%s'",
                attachment.getOriginalFileName(),
                formatFileSize(attachment.getFileSize()),
                project.getProjectName());

        activityLogService.logAction(
                uploader,
                "FILE_UPLOADED",
                "Project",
                project.getId(),
                details
        );

        return attachment;
    }

    /**
     * Upload file to task
     */
    public Attachment uploadToTask(Long taskId, Long userId, MultipartFile file, String description)
            throws IOException {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Attachment attachment = saveFile(file, uploader, null, task, description);

       
        String details = String.format("Uploaded file '%s' (%s) to task '%s' in project '%s'",
                attachment.getOriginalFileName(),
                formatFileSize(attachment.getFileSize()),
                task.getTaskName(),
                task.getProject() != null ? task.getProject().getProjectName() : "N/A");

        activityLogService.logAction(
                uploader,
                "FILE_UPLOADED",
                "Task",
                task.getId(),
                details
        );

        return attachment;
    }

    /**
     * Core file saving logic
     */
    private Attachment saveFile(MultipartFile file, User user, Project project, Task task, String description)
            throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null ?
                originalFileName.substring(originalFileName.lastIndexOf(".")) : "";
        String uniqueFileName = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        Attachment attachment = new Attachment();
        attachment.setFileName(uniqueFileName);
        attachment.setOriginalFileName(originalFileName);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFilePath(UPLOAD_DIR + uniqueFileName);
        attachment.setDescription(description);
        attachment.setUploadedBy(user);
        attachment.setProject(project);
        attachment.setTask(task);
        attachment.setUploadedAt(LocalDateTime.now());

        return attachmentRepository.save(attachment);
    }

    /**
     * Get all attachments for a project
     */
    @Transactional(readOnly = true)
    public List<Attachment> getProjectAttachments(Long projectId) {
        return attachmentRepository.findByProjectId(projectId);
    }

    /**
     * Get all attachments for a task
     */
    @Transactional(readOnly = true)
    public List<Attachment> getTaskAttachments(Long taskId) {
        return attachmentRepository.findByTaskId(taskId);
    }

    /**
     * Get attachment by ID
     */
    @Transactional(readOnly = true)
    public Optional<Attachment> getAttachment(Long id) {
        return attachmentRepository.findById(id);
    }

    /**
     * Delete attachment
     */
    public void deleteAttachment(Long id) throws IOException {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + id));

        Path filePath = Paths.get(attachment.getFilePath());
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
        String entityType = null;
        Long entityId = null;
        String entityName = "Unknown";

        if (attachment.getProject() != null) {
            entityType = "Project";
            entityId = attachment.getProject().getId();
            entityName = attachment.getProject().getProjectName();
        } else if (attachment.getTask() != null) {
            entityType = "Task";
            entityId = attachment.getTask().getId();
            entityName = attachment.getTask().getTaskName();
        }

        attachmentRepository.delete(attachment);

        User uploader = attachment.getUploadedBy();
        if (uploader != null && entityType != null) {
            String details = String.format("Deleted file '%s' (%s) from %s '%s'",
                    attachment.getOriginalFileName(),
                    formatFileSize(attachment.getFileSize()),
                    entityType, entityName);

            activityLogService.logAction(
                    uploader,
                    "FILE_DELETED",
                    entityType,
                    entityId,
                    details
            );
        }
    }

    /**
     * Download attachment file (returns path)
     */
    @Transactional(readOnly = true)
    public Path getAttachmentFile(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found: " + id));

        return Paths.get(attachment.getFilePath());
    }
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}