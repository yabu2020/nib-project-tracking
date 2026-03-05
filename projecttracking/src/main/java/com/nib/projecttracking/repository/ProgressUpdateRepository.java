
package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.ProgressUpdate;
import com.nib.projecttracking.entity.Project;
import com.nib.projecttracking.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProgressUpdateRepository extends JpaRepository<ProgressUpdate, Long> {
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByUser(User user);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.user.id = :userId")
    List<ProgressUpdate> findByUserId(@Param("userId") Long userId);
    
   
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findBySubmittedBy(User submittedBy);
    
   
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById")
    List<ProgressUpdate> findBySubmittedById(@Param("submittedById") Long submittedById);
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedToManagement = true")
    List<ProgressUpdate> findBySubmittedToManagementTrue();
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.submittedToManagement = true")
    List<ProgressUpdate> findBySubmittedByIdAndSubmittedToManagementTrue(@Param("submittedById") Long submittedById);
    
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByProject(Project project);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.project.id = :projectId")
    List<ProgressUpdate> findByProjectId(@Param("projectId") Long projectId);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByUserAndProject(User user, Project project);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.user.id = :userId AND p.project.id = :projectId")
    List<ProgressUpdate> findByUserIdAndProjectId(@Param("userId") Long userId, @Param("projectId") Long projectId);
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findBySubmittedByAndProject(User submittedBy, Project project);
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.project.id = :projectId")
    List<ProgressUpdate> findBySubmittedByIdAndProjectId(
        @Param("submittedById") Long submittedById, 
        @Param("projectId") Long projectId);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByUpdateDateBetween(LocalDate startDate, LocalDate endDate);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByUserAndUpdateDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.user.id = :userId AND p.updateDate BETWEEN :startDate AND :endDate")
    List<ProgressUpdate> findByUserIdAndUpdateDateBetween(
        @Param("userId") Long userId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
   
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findBySubmittedByAndUpdateDateBetween(User submittedBy, LocalDate startDate, LocalDate endDate);
    
 
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.updateDate BETWEEN :startDate AND :endDate")
    List<ProgressUpdate> findBySubmittedByIdAndUpdateDateBetween(
        @Param("submittedById") Long submittedById, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    List<ProgressUpdate> findByUpdateDateAfterOrderByUpdateDateDesc(LocalDate date);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.user.id = :userId AND p.updateDate > :date ORDER BY p.updateDate DESC")
    List<ProgressUpdate> findByUserIdAndUpdateDateAfterOrderByUpdateDateDesc(
        @Param("userId") Long userId, 
        @Param("date") LocalDate date);
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.updateDate > :date ORDER BY p.updateDate DESC")
    List<ProgressUpdate> findBySubmittedByIdAndUpdateDateAfterOrderByUpdateDateDesc(
        @Param("submittedById") Long submittedById, 
        @Param("date") LocalDate date);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    List<ProgressUpdate> findUpdatesWithBlockers();
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.user.id = :userId AND p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    List<ProgressUpdate> findUpdatesWithBlockersByUserId(@Param("userId") Long userId);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.updateDate BETWEEN :startDate AND :endDate AND p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    List<ProgressUpdate> findUpdatesWithBlockersByDateRange(
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT p FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    List<ProgressUpdate> findUpdatesWithBlockersBySubmittedById(@Param("submittedById") Long submittedById);
    
    
    long countByUser(User user);
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
    
    long countByProject(Project project);
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.project.id = :projectId")
    long countByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    long countUpdatesWithBlockers();
    // Add this method to ProgressUpdateRepository.java

@Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null' AND (p.blockerStatus IS NULL OR p.blockerStatus != 'SOLVED')")
long countActiveBlockers();
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.user.id = :userId AND p.blockers IS NOT NULL AND p.blockers != '' AND p.blockers != 'null'")
    long countUpdatesWithBlockersByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.project.id = :projectId " +
       "AND p.blockers IS NOT NULL AND p.blockers != '' " +
       "AND p.updateDate BETWEEN :startDate AND :endDate " +
       "AND (p.blockerStatus IS NULL OR p.blockerStatus != 'SOLVED')")
long countActiveBlockersByProjectAndDateRange(
    @Param("projectId") Long projectId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
   
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById")
    long countBySubmittedById(@Param("submittedById") Long submittedById);
    
    
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById AND p.submittedToManagement = true")
    long countBySubmittedByIdAndSubmittedToManagementTrue(@Param("submittedById") Long submittedById);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.project.id = :projectId AND p.updateDate BETWEEN :startDate AND :endDate")
    long countByProjectIdAndDateRange(
        @Param("projectId") Long projectId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT COUNT(p) FROM ProgressUpdate p WHERE p.project.id = :projectId AND p.updateDate BETWEEN :startDate AND :endDate AND p.blockers IS NOT NULL AND p.blockers != ''")
    long countByProjectIdAndBlockers(
        @Param("projectId") Long projectId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    @EntityGraph(attributePaths = {"submittedBy", "project"})
    @Query("SELECT pu FROM ProgressUpdate pu WHERE pu.project.id = :projectId AND pu.updateDate BETWEEN :startDate AND :endDate ORDER BY pu.updateDate DESC, pu.createdAt DESC")
    List<ProgressUpdate> findUpdatesByProjectAndDateRange(
        @Param("projectId") Long projectId, 
        @Param("startDate") LocalDate startDate, 
        @Param("endDate") LocalDate endDate);
    
    void deleteByUser(User user);
    
    @Query("DELETE FROM ProgressUpdate p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    void deleteByProject(Project project);
    
    @Query("DELETE FROM ProgressUpdate p WHERE p.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
    
    @Query("DELETE FROM ProgressUpdate p WHERE p.submittedBy.id = :submittedById")
    void deleteBySubmittedById(@Param("submittedById") Long submittedById);
}