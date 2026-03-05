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
    
    /**
     * ✅ Get all root comments for a project (no parent) WITH REPLIES LOADED
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getProjectComments(Long projectId) {
        List<ProjectComment> rootComments = commentRepository.findByProjectIdAndParentCommentIsNull(projectId);
        
        for (ProjectComment comment : rootComments) {
            if (comment.getReplies() != null) {
                comment.getReplies().size();
            }
        }
        
        return rootComments;
    }
    
    /**
     * ✅ Add a new comment or reply
     */
    public ProjectComment addComment(Long projectId, Long userId, String commentText, Long parentCommentId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        ProjectComment comment = new ProjectComment();
        comment.setProject(project);
        comment.setUser(user);
        comment.setCommentText(commentText);
        if (parentCommentId != null) {
            ProjectComment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Parent comment not found: " + parentCommentId));
            comment.setParentComment(parentComment);
        }
        
        return commentRepository.save(comment);
    }
    
    /**
     * ✅ Get all replies for a comment
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getCommentReplies(Long commentId) {
        return commentRepository.findByParentCommentId(commentId);
    }
    
    /**
     * ✅ Delete a comment
     */
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found: " + commentId);
        }
        commentRepository.deleteById(commentId);
    }
    
    /**
     * ✅ Get comment by ID
     */
    @Transactional(readOnly = true)
    public Optional<ProjectComment> getCommentById(Long commentId) {
        return commentRepository.findById(commentId);
    }
    
    /**
     * ✅ Get all comments for a project (including replies)
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getAllCommentsForProject(Long projectId) {
        return commentRepository.findByProjectId(projectId);
    }
    
    /**
     * ✅ Get root comments with replies (for hierarchical display)
     */
    @Transactional(readOnly = true)
    public List<ProjectComment> getRootCommentsWithReplies(Long projectId) {
        return commentRepository.findRootCommentsWithRepliesByProjectId(projectId);
    }
}