package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>,
        JpaSpecificationExecutor<ActivityLog> {


    List<ActivityLog> findByUserId(Long userId);

    List<ActivityLog> findByTimestampAfterOrderByTimestampDesc(LocalDateTime since);

    

    
    @Query("SELECT DISTINCT a.action FROM ActivityLog a WHERE a.action IS NOT NULL ORDER BY a.action")
    List<String> findDistinctActions();

    @Query("SELECT DISTINCT a.entityType FROM ActivityLog a WHERE a.entityType IS NOT NULL ORDER BY a.entityType")
    List<String> findDistinctEntityTypes();

    @Query("SELECT DISTINCT a.action FROM ActivityLog a WHERE a.user.id = :userId AND a.action IS NOT NULL ORDER BY a.action")
    List<String> findDistinctActionsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT a.entityType FROM ActivityLog a WHERE a.user.id = :userId AND a.entityType IS NOT NULL ORDER BY a.entityType")
    List<String> findDistinctEntityTypesByUserId(@Param("userId") Long userId);

    

    @Query("SELECT a.action, COUNT(a) FROM ActivityLog a GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction();

    @Query("SELECT a.entityType, COUNT(a) FROM ActivityLog a GROUP BY a.entityType ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntityType();

    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    

    @Query("SELECT DISTINCT a FROM ActivityLog a " +
           "LEFT JOIN FETCH a.user u " +  
           "WHERE (:userId IS NULL OR u.id = :userId) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:start IS NULL OR a.timestamp >= :start) " +
           "AND (:end IS NULL OR a.timestamp <= :end)")
    Page<ActivityLog> findFilteredLogs(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);



    @Query("SELECT DISTINCT a FROM ActivityLog a " +
           "LEFT JOIN FETCH a.user u " +
           "WHERE (:userId IS NULL OR u.id = :userId) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:start IS NULL OR a.timestamp >= :start) " +
           "AND (:end IS NULL OR a.timestamp <= :end) " +
           "ORDER BY a.timestamp DESC")
    List<ActivityLog> findFilteredLogsForExport(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);


    @Query("SELECT a FROM ActivityLog a " +
           "LEFT JOIN FETCH a.user " +
           "WHERE a.entityType = :entityType AND a.entityId = :entityId " +
           "ORDER BY a.timestamp DESC")
    List<ActivityLog> findByEntity(@Param("entityType") String entityType,
                                   @Param("entityId") Long entityId);

   

    List<ActivityLog> findByActionOrderByTimestampDesc(String action);

    List<ActivityLog> findByEntityTypeOrderByTimestampDesc(String entityType);



    @Query("SELECT COUNT(a) FROM ActivityLog a WHERE a.timestamp >= :since")
    long countRecentLogs(@Param("since") LocalDateTime since);
}