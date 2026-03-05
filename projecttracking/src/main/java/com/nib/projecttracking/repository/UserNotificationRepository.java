package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {
    
    List<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);
    
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    long countByUserIdAndReadFalse(Long userId);
    
    List<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);
    
    @Query("SELECT n FROM UserNotification n WHERE n.user.id = ?1 AND n.createdAt >= ?2 ORDER BY n.createdAt DESC")
    List<UserNotification> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);
    
    @Query("DELETE FROM UserNotification n WHERE n.createdAt < ?1")
    void deleteOldNotifications(LocalDateTime cutoffDate);
    
    
    boolean existsByUserIdAndTypeAndRelatedEntityId(Long userId, String type, Long relatedEntityId);
    
    long countByUserId(Long userId);
}