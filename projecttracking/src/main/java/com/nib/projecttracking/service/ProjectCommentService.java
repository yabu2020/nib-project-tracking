package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.ProjectComment;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.repository.ProjectCommentRepository;
import com.nib.projecttracking.repository.ProjectRepository;
import com.nib.projecttracking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectCommentService {
    
    @Autowired
    private ProjectCommentRepository commentRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserNotificationService userNotificationService;
    
    /**
     * Get all root comments for a project (no parent) WITH REPLIES LOADED
     */
   /**
 * Get ALL comments for a project (including replies) - for chat modal
 */
@Transactional(readOnly = true)
public List<ProjectComment> getAllCommentsForProject(Long projectId) {
    System.out.println("📂 Fetching ALL comments for project: " + projectId);
    
    // Fetch all comments from database
    List<ProjectComment> allComments = commentRepository.findByProjectId(projectId);
    
    System.out.println("📊 Found " + allComments.size() + " total comments");
    
    // Force load parent comments for replies
    for (ProjectComment comment : allComments) {
        if (comment.getParentComment() != null) {
            // Trigger lazy loading of parent
            Long parentId = comment.getParentComment().getId();
            System.out.println("💬 Comment " + comment.getId() + " is a reply to parent " + parentId);
        } else {
            System.out.println("📝 Comment " + comment.getId() + " is a root comment");
        }
    }
    
    return allComments;
}
    
    /**
     * Get ALL comments for a project (including replies) - for chat modal
     */
 
    /**
     * Add a new comment or reply + send notifications
     */
    @Transactional
    public ProjectComment addComment(Long projectId, Long userId, String commentText, Long parentCommentId) {
        System.out.println("\n📝=== ADDING COMMENT ===");
        System.out.println("Project ID: " + projectId);
        System.out.println("User ID (commenter): " + userId);
        System.out.println("Comment text: " + commentText);
        System.out.println("Parent comment ID: " + parentCommentId);
        
        // Load project
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> {
                System.err.println("❌ Project not found: " + projectId);
                return new RuntimeException("Project not found: " + projectId);
            });
        
        // Load user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                System.err.println("❌ User not found: " + userId);
                return new RuntimeException("User not found: " + userId);
            });
        
        System.out.println("Project: " + project.getProjectName());
        System.out.println("Project Manager: " + (project.getManager() != null ? project.getManager().getUsername() : "NULL"));
        System.out.println("Project Initiator: " + (project.getInitiatedBy() != null ? project.getInitiatedBy().getUsername() : "NULL"));
        System.out.println("Commenter: " + user.getUsername());
        
        // Create new comment
        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setUser(user);
        comment.setCommentText(commentText);
        
        // If this is a reply, set the parent comment
        if (parentCommentId != null) {
            System.out.println("📎 This is a REPLY to comment: " + parentCommentId);
            ProjectComment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> {
                    System.err.println("❌ Parent comment not found: " + parentCommentId);
                    return new RuntimeException("Parent comment not found: " + parentCommentId);
                });
            comment.setParentComment(parentComment);
        } else {
            System.out.println("📝 This is a ROOT comment (no parent)");
        }
        
        // Save the comment
        ProjectComment savedComment = commentRepository.save(comment);
        System.out.println("✅ Comment saved with ID: " + savedComment.getId());
        
        // Send notifications for ALL comments (root AND replies)
        System.out.println("🔔 Sending notifications for comment...");
        sendCommentNotifications(project, user, userId, commentText, savedComment.getId(), parentCommentId);
        
        return savedComment;
    }
    
    /**
     * Send notifications to manager, initiator, parent author, and team members
     * Order: Parent Author (REPLY) → Manager (COMMENT) → Initiator (COMMENT) → Team Members (COMMENT)
     */
    private void sendCommentNotifications(Project project, User commenter, Long commenterId, 
                                         String commentText, Long commentId, Long parentCommentId) {
        System.out.println("\n📤=== SENDING COMMENT NOTIFICATIONS ===");
        
        // Track who we've already notified for COMMENT type (to avoid duplicates)
        Set<Long> notifiedForComment = new HashSet<>();
        notifiedForComment.add(commenterId); // Don't notify the commenter themselves
        
        // Create comment preview (first 100 chars)
        String preview = commentText != null && commentText.length() > 100 
            ? commentText.substring(0, 100) + "..." 
            : commentText;
        
        // ✅ 1. FIRST: Notify parent comment author with REPLY notification (for replies)
        // This ensures parent author gets REPLY notification BEFORE being considered for COMMENT notification
        if (parentCommentId != null) {
            try {
                ProjectComment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));
                
                User parentAuthor = parentComment.getUser();
                
                if (!parentAuthor.getId().equals(commenterId)) {
                    System.out.println("✅ Creating REPLY notification for parent comment author: " + parentAuthor.getUsername());
                    System.out.println("🔔 Calling createReplyNotification...");
                    
                    userNotificationService.createReplyNotification(
                        parentAuthor,
                        commenter,
                        project.getProjectName(),
                        project.getId(),
                        preview,
                        commentId
                    );
                    
                    // Add parent author to notifiedForComment set to prevent duplicate COMMENT notification
                    notifiedForComment.add(parentAuthor.getId());
                    
                    System.out.println("✅✓✓ Notified parent comment author with REPLY");
                    System.out.println("📝 Added " + parentAuthor.getUsername() + " to notifiedForComment set");
                } else {
                    System.out.println("⚠️ Skipping parent author notification: is the same as commenter (no self-notification)");
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to notify parent comment author: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ This is a root comment - no parent author to notify");
        }
        
        // ✅ 2. SECOND: Notify project manager with COMMENT notification (if exists AND not already notified)
        if (project.getManager() != null && !notifiedForComment.contains(project.getManager().getId())) {
            try {
                System.out.println("✅ Creating COMMENT notification for manager: " + project.getManager().getUsername());
                userNotificationService.createCommentNotification(
                    project.getManager(),
                    commenter,
                    project.getProjectName(),
                    project.getId(),
                    preview,
                    commentId
                );
                notifiedForComment.add(project.getManager().getId());
                System.out.println("✅✓✓ Notified manager with COMMENT");
            } catch (Exception e) {
                System.err.println("❌ Failed to notify manager: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (project.getManager() == null) {
                System.out.println("⚠️ Skipping manager notification: project has no manager");
            } else if (notifiedForComment.contains(project.getManager().getId())) {
                System.out.println("⚠️ Skipping manager notification: manager already notified (likely as parent author)");
            } else {
                System.out.println("⚠️ Skipping manager notification: manager IS the commenter");
            }
        }
        
        // ✅ 3. THIRD: Notify project initiator with COMMENT notification (if different and not already notified)
        if (project.getInitiatedBy() != null 
            && !notifiedForComment.contains(project.getInitiatedBy().getId())) {
            try {
                System.out.println("✅ Creating COMMENT notification for initiator: " + project.getInitiatedBy().getUsername());
                userNotificationService.createCommentNotification(
                    project.getInitiatedBy(),
                    commenter,
                    project.getProjectName(),
                    project.getId(),
                    preview,
                    commentId
                );
                notifiedForComment.add(project.getInitiatedBy().getId());
                System.out.println("✅✓✓ Notified initiator with COMMENT");
            } catch (Exception e) {
                System.err.println("❌ Failed to notify initiator: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ Skipping initiator notification: already notified or is commenter");
        }
        
        // ✅ 4. FOURTH: Notify ALL project team members with COMMENT notification (if not already notified)
        try {
            System.out.println("🔍 Finding project team members for project: " + project.getId());
            List<User> projectTeam = userRepository.findUsersByProjectId(project.getId(), commenterId);
            System.out.println("👥 Found " + projectTeam.size() + " team members");
            
            for (User teamMember : projectTeam) {
                if (!notifiedForComment.contains(teamMember.getId())) {
                    System.out.println("✅ Creating COMMENT notification for team member: " + teamMember.getUsername());
                    userNotificationService.createCommentNotification(
                        teamMember,
                        commenter,
                        project.getProjectName(),
                        project.getId(),
                        preview,
                        commentId
                    );
                    notifiedForComment.add(teamMember.getId());
                    System.out.println("✅✓✓ Notified team member with COMMENT");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to notify team members: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("📤=== END NOTIFICATIONS ===\n");
    }
    
    /**
     * Get all replies for a comment
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getCommentReplies(Long commentId) {
        System.out.println("📂 Fetching replies for comment: " + commentId);
        List<ProjectComment> replies = commentRepository.findByParentCommentId(commentId);
        System.out.println("📊 Found " + replies.size() + " replies");
        return replies;
    }
    
    /**
     * Delete a comment
     */
    public void deleteComment(Long commentId) {
        System.out.println("🗑️ Deleting comment: " + commentId);
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found: " + commentId);
        }
        commentRepository.deleteById(commentId);
        System.out.println("✅ Comment deleted");
    }
    
    /**
     * Get comment by ID
     */
    @Transactional(readOnly = true)
    public Optional<ProjectComment> getCommentById(Long commentId) {
        System.out.println("🔍 Fetching comment by ID: " + commentId);
        return commentRepository.findById(commentId);
    }
    
    /**
     * Get root comments with replies (for hierarchical display)
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getRootCommentsWithReplies(Long projectId) {
        System.out.println("📂 Fetching root comments with replies for project: " + projectId);
        return commentRepository.findRootCommentsWithRepliesByProjectId(projectId);
    }
}