package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.ProjectComment;
import com.nib.projecttracking.service.ProjectCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ProjectCommentController {
    
    @Autowired
    private ProjectCommentService commentService;
    
    @GetMapping
    public ResponseEntity<List<ProjectComment>> getComments(@PathVariable Long projectId) {
        return ResponseEntity.ok(commentService.getProjectComments(projectId));
    }
    
    @PostMapping
    public ResponseEntity<ProjectComment> addComment(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestParam String commentText,
            @RequestParam(required = false) Long parentCommentId) {
        
        ProjectComment comment = commentService.addComment(projectId, userId, commentText, parentCommentId);
        return ResponseEntity.ok(comment);
    }
    
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<ProjectComment>> getReplies(@PathVariable Long commentId) {
        return ResponseEntity.ok(commentService.getCommentReplies(commentId));
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}