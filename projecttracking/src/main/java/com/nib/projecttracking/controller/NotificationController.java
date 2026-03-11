package com.nib.projecttracking.controller;

import com.nib.projecttracking.service.EmailService;
import com.nib.projecttracking.service.NotificationSchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://localhost:5174"})
public class NotificationController {
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NotificationSchedulerService notificationScheduler;
    
    @PostMapping("/test-email")
    public ResponseEntity<?> testEmail(@RequestParam String email) {
        try {
            emailService.sendTestEmail(email);
            return ResponseEntity.ok(Map.of("message", "Test email sent to " + email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }
    
    @PostMapping("/trigger-overdue-check")
    
    public ResponseEntity<?> triggerOverdueCheck() {
        try {
            notificationScheduler.triggerNotificationsManually();
            return ResponseEntity.ok(Map.of("message", "Notification check triggered"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed: " + e.getMessage()));
        }
    }
    // In NotificationController.java
@PostMapping("/test-trigger")
public ResponseEntity<?> testTrigger() {
    System.out.println("🔧 Test trigger called");
    notificationScheduler.triggerNotificationsManually();
    return ResponseEntity.ok(Map.of("message", "Check backend console for results"));
}


}