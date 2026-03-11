package com.nib.projecttracking.controller;

import com.nib.projecttracking.entity.ProjectComment;
import com.nib.projecttracking.service.ProjectCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects/{projectId}/comments")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class ProjectCommentController {
    
    @Autowired
    private ProjectCommentService commentService;
    
   @GetMapping
public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long projectId) {
    System.out.println("📝 Fetching comments for project: " + projectId);
    
    List<ProjectComment> allComments = commentService.getAllCommentsForProject(projectId);
    System.out.println("📊 Found " + allComments.size() + " total comments");
    
    List<Map<String, Object>> response = allComments.stream().map(comment -> {
        Map<String, Object> map = new HashMap<>();
        map.put("id", comment.getId());
        map.put("commentText", comment.getCommentText());
        
        
        Long parentId = null;
        if (comment.getParentComment() != null) {
            parentId = comment.getParentComment().getId();
            System.out.println("📎 Comment " + comment.getId() + " is a REPLY to parent " + parentId);
        } else {
            System.out.println("📝 Comment " + comment.getId() + " is a ROOT comment");
        }
        map.put("parentCommentId", parentId);
        
        map.put("userName", comment.getUserName());
        map.put("userId", comment.getUserId());
        map.put("userRole", comment.getUserRole());
        map.put("projectId", comment.getProjectId());
        map.put("projectName", comment.getProjectName());
        map.put("createdAt", comment.getCreatedAt());
        map.put("updatedAt", comment.getUpdatedAt());
        
        return map;
    }).collect(Collectors.toList());
    
    System.out.println("✅ Returning " + response.size() + " comments to frontend");
    return ResponseEntity.ok(response);
}
    @PostMapping
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long projectId,
            @RequestParam Long userId,
            @RequestParam String commentText,
            @RequestParam(required = false) Long parentCommentId) {
        
        System.out.println("📝 Adding comment - Project: " + projectId + ", User: " + userId + ", Parent: " + parentCommentId);
        
        ProjectComment comment = commentService.addComment(projectId, userId, commentText, parentCommentId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("id", comment.getId());
        response.put("commentText", comment.getCommentText());
        response.put("parentCommentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null);
        response.put("userName", comment.getUserName());
        response.put("userId", comment.getUserId());
        response.put("userRole", comment.getUserRole());
        response.put("projectId", comment.getProjectId());
        response.put("projectName", comment.getProjectName());
        response.put("createdAt", comment.getCreatedAt());
        
        System.out.println("✅ Comment created with ID: " + comment.getId() + ", parentCommentId: " + response.get("parentCommentId"));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<List<Map<String, Object>>> getReplies(@PathVariable Long commentId) {
        List<ProjectComment> replies = commentService.getCommentReplies(commentId);
        
        List<Map<String, Object>> response = replies.stream().map(reply -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", reply.getId());
            map.put("commentText", reply.getCommentText());
            map.put("parentCommentId", reply.getParentComment() != null ? reply.getParentComment().getId() : null);
            map.put("userName", reply.getUserName());
            map.put("userId", reply.getUserId());
            map.put("userRole", reply.getUserRole());
            map.put("createdAt", reply.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully"));
    }
}