package com.nib.projecttracking.controller;
import com.nib.projecttracking.entity.User;
import com.nib.projecttracking.entity.UserNotification;
import com.nib.projecttracking.service.UserNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class UserNotificationController {
    
    @Autowired
    private UserNotificationService notificationService;
    

    @GetMapping("/unread")
    public ResponseEntity<List<UserNotification>> getUnreadNotifications(@RequestParam Long userId) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }
    
    @GetMapping
    public ResponseEntity<List<UserNotification>> getAllNotifications(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.getAllNotifications(userId, limit));
    }
    
    
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@RequestParam Long userId) {
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
    
   
    @PutMapping("/{id}/read")
    public ResponseEntity<UserNotification> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }
    
    
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@RequestParam Long userId) {
        long marked = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of(
            "message", "All notifications marked as read",
            "markedCount", marked
        ));
    }
    

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted"));
    }
    

    @PostMapping("/test")
    public ResponseEntity<UserNotification> createTestNotification(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "HIGH") String priority) {
        
        UserNotification notification = notificationService.createNotification(
            new User() {{ setId(userId); }},
            "🧪 Test Notification",
            "This is a test in-app notification. Priority: " + priority,
            "TEST",
            priority
        );
        
        return ResponseEntity.ok(notification);
    }
}