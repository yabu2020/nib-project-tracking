package com.nib.projecttracking.repository;

import com.nib.projecttracking.entity.Milestone;
import com.nib.projecttracking.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
    
    
    List<Milestone> findByProject(Project project);
    @Query("SELECT m FROM Milestone m WHERE m.project.id = :projectId")
List<Milestone> findByProjectId(@Param("projectId") Long projectId);
    
    List<Milestone> findByStatus(Milestone.MilestoneStatus status);
    
    
    @Query("SELECT m FROM Milestone m LEFT JOIN FETCH m.project WHERE m.project = ?1 AND m.status = 'COMPLETED'")
    List<Milestone> findCompletedMilestonesByProject(Project project);
    
    @Query("SELECT m FROM Milestone m LEFT JOIN FETCH m.project " +
           "WHERE m.targetDate < :today AND m.status NOT IN :excludedStatuses")
    List<Milestone> findOverdueMilestones(@Param("today") LocalDate today,
                                          @Param("excludedStatuses") List<Milestone.MilestoneStatus> excludedStatuses);
    
    
    @Query("SELECT m FROM Milestone m LEFT JOIN FETCH m.project " +
           "WHERE m.targetDate BETWEEN :startDate AND :endDate AND m.status != 'COMPLETED'")
    List<Milestone> findUpcomingMilestones(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);
    
    
    @Query("SELECT m FROM Milestone m LEFT JOIN FETCH m.project " +
           "WHERE m.targetDate BETWEEN :startDate AND :endDate ORDER BY m.targetDate")
    List<Milestone> findMilestonesByDateRange(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
@Query("SELECT m FROM Milestone m WHERE m.project.id = :projectId AND (m.targetDate BETWEEN :startDate AND :endDate OR m.actualDate BETWEEN :startDate AND :endDate) ORDER BY m.targetDate ASC")
List<Milestone> findMilestonesByDateRange(@Param("projectId") Long projectId, 
                                          @Param("startDate") LocalDate startDate, 
                                          @Param("endDate") LocalDate endDate);
    
    @Query("SELECT m FROM Milestone m LEFT JOIN FETCH m.project")
    @Override
    List<Milestone> findAll();
}
