package com.nib.projecttracking.service;

import com.nib.projecttracking.entity.User; 
import com.nib.projecttracking.entity.UserNotification;
import com.nib.projecttracking.repository.UserNotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;  
import java.util.List;

@Service
@Transactional
public class UserNotificationService {
    
    @Autowired
    private UserNotificationRepository notificationRepository;
    
    /**
     * Create a new notification for a user
     */
    public UserNotification createNotification(User user, String title, String message, 
                                               String type, String priority) {
        return createNotification(user, title, message, type, priority, null, null);
    }
    
    /**
     * Create notification with related entity reference
     */
    public UserNotification createNotification(User user, String title, String message,
                                               String type, String priority,
                                               String relatedEntityType, Long relatedEntityId) {
        
        UserNotification notification = new UserNotification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setPriority(priority);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        
        return notificationRepository.save(notification);
    }
    
    /**
     * Create notification when a comment is added to a project
     */
    @Transactional
    public UserNotification createCommentNotification(
            User recipient,           
            User commenter,           
            String projectName,      
            Long projectId,           
            String commentPreview,   
            Long commentId) {         
        
        System.out.println("\n🔔=== CREATING COMMENT NOTIFICATION ===");
        System.out.println("Recipient: " + recipient.getUsername() + " (ID: " + recipient.getId() + ")");
        System.out.println("Commenter: " + commenter.getUsername());
        System.out.println("Project: " + projectName + " (ID: " + projectId + ")");
        System.out.println("Comment preview: " + commentPreview);
        System.out.println("Comment ID: " + commentId);
        
    
        String commenterName = commenter.getFullName() != null ? commenter.getFullName() : commenter.getUsername();
        String title = "New Comment on " + projectName;
        String message = commenterName + " commented: \"" + commentPreview + "\"";
        
       
        return createNotification(
            recipient,
            title,
            message,
            "COMMENT",
            "MEDIUM",
            "Project",
            projectId
        );
    }
    /**
 * Create notification when someone replies to your comment
 */
@Transactional
public UserNotification createReplyNotification(
        User recipient,          
        User replier,             
        String projectName,       
        Long projectId,           
        String replyPreview,    
        Long commentId) {         
    
    System.out.println("\n🔔=== CREATING REPLY NOTIFICATION ===");
    System.out.println("Recipient: " + recipient.getUsername() + " (ID: " + recipient.getId() + ")");
      System.out.println("✅ createReplyNotification WAS CALLED!");  
    System.out.println("Type will be: COMMENT_REPLY");
    System.out.println("Replier: " + replier.getUsername());
    System.out.println("Project: " + projectName + " (ID: " + projectId + ")");
    System.out.println("Reply preview: " + replyPreview);
    System.out.println("Notification Type: COMMENT_REPLY");  
    
    String replierName = replier.getFullName() != null ? replier.getFullName() : replier.getUsername();
    String title = "New Reply on " + projectName;
    String message = replierName + " replied to your comment: \"" + replyPreview + "\"";
    
   
    UserNotification notification = new UserNotification();
    notification.setUser(recipient);
    notification.setTitle(title);
    notification.setMessage(message);
    notification.setType("COMMENT_REPLY");  
    notification.setPriority("MEDIUM");
    notification.setRelatedEntityType("Project");
    notification.setRelatedEntityId(projectId);
    notification.setRead(false);
    
    UserNotification saved = notificationRepository.save(notification);
    System.out.println("✅ Reply notification saved with ID: " + saved.getId());
    System.out.println("✅ Notification type in DB: " + saved.getType());
    
    return saved;
}
    
    /**
     * Get unread notifications for a user
     */
    @Transactional(readOnly = true)
    public List<UserNotification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }
    
    /**
     * Get all notifications for a user (with limit)
     */
    @Transactional(readOnly = true)
    public List<UserNotification> getAllNotifications(Long userId, int limit) {
        List<UserNotification> all = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return all.size() > limit ? all.subList(0, limit) : all;
    }
    
    /**
     * Count unread notifications
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }
    
    /**
     * Mark a notification as read
     */
    public UserNotification markAsRead(Long notificationId) {
        UserNotification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        notification.markAsRead();
        return notificationRepository.save(notification);
    }
    
    /**
     * Mark all notifications as read for a user
     */
    public long markAllAsRead(Long userId) {
        List<UserNotification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(UserNotification::markAsRead);
        notificationRepository.saveAll(unread);
        return unread.size();
    }
    
    /**
     * Delete a notification
     */
    public void deleteNotification(Long notificationId) {
        notificationRepository.deleteById(notificationId);
    }
    
    /**
     * Delete old notifications (cleanup)
     */
    public void cleanupOldNotifications(int daysToKeep) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteOldNotifications(cutoff);
        System.out.println("Cleaned up notifications older than " + daysToKeep + " days");
    }
}